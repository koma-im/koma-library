package koma

import koma.util.*
import koma.util.coroutine.adapter.okhttp.await
import koma.util.coroutine.adapter.okhttp.extract
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import mu.KotlinLogging
import okhttp3.*
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.LinkedHashMap
import kotlin.math.absoluteValue
import kotlin.system.measureTimeMillis

private val logger = KotlinLogging.logger {}

internal class Downloader(
        private val httpClient: OkHttpClient,
        /**
         * don't count slow items started earlier than given number of millisecs
         */
        ignoreOlderThan: Long = 11000,
        maxConcurrent: Int = 1
): CoroutineScope by CoroutineScope(Dispatchers.Default)  {
    private val chan = Channel<DlMsg>()
    private val inProgress = LinkedHashMap<HttpUrl, DownloadItem>()
    //not started
    // last in first out
    private val waiting = StackMap<HttpUrl, DownloadItem>()
    init {
        launch {
            for (m in chan) {
                when (m) {
                    is DlMsg.Get -> {
                        logger.trace { "requesting ${m.url}" }
                        val downloading = inProgress[m.url]
                        if (downloading != null)m.response.complete(downloading)
                        val w = waiting.get(m.url)
                        if (w != null) m.response.complete(w)
                        else {
                            val d = DownloadItem(m.url, m.maxStale)
                            if (inProgress.size > maxConcurrent) {
                                inProgress.entries.takeWhile { mutableEntry ->
                                    mutableEntry.value.startAt?.let {
                                        System.currentTimeMillis() - it > ignoreOlderThan
                                    } == true
                                }.map { it.key }.also {
                                    if (it.isNotEmpty())
                                        logger.debug { "ignoring slow media files $it" }
                                }.forEach { inProgress.remove(it) }
                            }
                            if (inProgress.size > maxConcurrent){
                                waiting.push(m.url, d)
                            } else {
                                inProgress[m.url] = d
                                d.start()
                            }
                            m.response.complete(d)
                        }
                    }
                    is DlMsg.Complete -> {
                        inProgress.remove(m.url)
                        while (inProgress.size < maxConcurrent) {
                            val (u, d) = waiting.popOrNull() ?: break
                            d.start()
                            inProgress[u] = d
                        }

                    }
                }
            }
        }
    }
    suspend fun downloadMedia(httpUrl: HttpUrl, maxStale: Int? = null): KResult<ByteArray, KomaFailure> {
        val v = CompletableDeferred<DownloadItem>()
        chan.send(DlMsg.Get(httpUrl, maxStale, v))
        val d = v.await()
        val i = d.value.await()
        return i

    }

    sealed class DlMsg{
        class Get(
                val url: HttpUrl,
                val maxStale: Int? = null,
                val response: CompletableDeferred<DownloadItem>
        ): DlMsg()
        // success or failure
        class Complete(val url: HttpUrl): DlMsg()
    }

    inner internal class DownloadItem(
            private val url: HttpUrl,
            private val maxStale: Int? = null
    ){
        val value = CompletableDeferred<KResult<ByteArray, KomaFailure>>()
        var startAt: Long? = null
        suspend fun start() = launch {
            startAt = System.currentTimeMillis()
            val req = Request.Builder().url(url).given(maxStale) {
                cacheControl(CacheControl
                        .Builder()
                        .maxStale(it, TimeUnit.SECONDS)
                        .build())
            }.build()
            logger.trace { "start download $url $maxStale" }
            val d = download(req)
            logger.trace { "complete download $url, with failure ${d.failureOrNull()}" }
            value.complete(d)
        }

        private suspend fun download(req: Request): KResult<ByteArray, KomaFailure> {
            val (s, f, r) = httpClient.newCall(req).await()
            if (r.testFailure(s, f)) {
                return KResult.failure( f)
            } else {
                val body = s.extract()
                val bs = body.map { it.bytes() }
                chan.send(DlMsg.Complete(url))
                return bs
            }

        }
    }
}

/**
 * concurrent access not allowed
 */
private class StackMap<K, V: Any>{
    private val keys = ArrayDeque<K>()
    private val map = mutableMapOf<K, V>()
    fun push(k: K, value: V) {
        val pfe = map.putIfAbsent(k, value)
        assert(pfe == null) { "stacking duplicate entry"}
        keys.push(k)
    }
    fun popOrNull(): Pair<K, V>?{
        if (keys.isEmpty()) return null
        val k =keys.pop()
        return k to map.remove(k)!!
    }
    fun get(key: K): V? {
        return map[key]
    }
}