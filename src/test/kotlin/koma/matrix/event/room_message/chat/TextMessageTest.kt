package koma.matrix.event.room_message.chat

import koma.matrix.json.jsonDefault
import koma.matrix.json.jsonDefaultConf
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.SerializersModule
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows

internal class TextMessageTest {

    @Test
    fun testOtherMessage() {
        val text = UnrecognizedMessage(JsonObject(mapOf("a" to JsonPrimitive("b"), "c" to JsonPrimitive("d)"))))
        val textJ = jsonDefault.encodeToString(M_Message.serializer(), text)
        assertEquals("""{"a":"b","c":"d)"}""", textJ)
        val unk = jsonDefault.decodeFromString(M_Message.serializer(), """{"a":"b","body":"d"}""")
        assertEquals("\"b\"", (unk as UnrecognizedMessage).raw["a"].toString())
        assertEquals("\"d\"", unk.raw["body"].toString())
        jsonDefault.decodeFromString(M_Message.serializer(), """{"body":"b1","msgtype":"m.!"}""")
    }

    @Serializable
    private data class TestingMessage(
            val body: String,
            val formatted_body: String? = null,
            val msgtype: String = "m.testing",
            val format: String? = null
    )
    @Serializable
    sealed class BasicMessage() {
        @Serializable
        class SubMessage(val message: String): BasicMessage()
    }

    sealed class SMessage() {
        @Serializable
        class SubMessage(val message: String): SMessage()

        @Serializer(forClass = SMessage::class)
        companion object : KSerializer<SMessage> {
            override val descriptor: SerialDescriptor =
                    PrimitiveSerialDescriptor("SMessage", PrimitiveKind.STRING)

            override fun serialize(encoder: Encoder, obj: SMessage) {
                val output = encoder as? JsonEncoder ?: throw SerializationException("This class can be saved only by Json, not $encoder")
                val tree = when (obj) {
                    is SubMessage -> output.json.encodeToJsonElement(SubMessage.serializer(), obj)
                }
                output.encodeJsonElement(tree)
            }

            override fun deserialize(decoder: Decoder): SMessage {
                val input = decoder as? JsonDecoder ?: throw SerializationException("This class can be loaded only by Json")
                val tree = input.decodeJsonElement() as? JsonObject ?: throw SerializationException("Expected JsonObject")
                return input.json.decodeFromJsonElement(SubMessage.serializer(), tree)
            }
        }
    }

    @Serializable
    class SWrapper(val smessage: SMessage)

    @Test
    fun testDeserialize() {
        var value = JsonObject(mapOf("body" to JsonPrimitive("msg0t")))
        val tt = jsonDefault.decodeFromJsonElement(TestingMessage.serializer(), value)
        assertEquals("msg0t", tt.body)
        assertEquals("m.testing", tt.msgtype)
        assertNull(tt.format)

        value =  JsonObject(mapOf("message" to JsonPrimitive("hello")))
        val tsub = jsonDefault.decodeFromJsonElement(BasicMessage.SubMessage.serializer(), value)
        assertEquals("hello", tsub.message)

       value =  JsonObject(mapOf("message" to JsonPrimitive("hello")))
        val ssm = jsonDefault.decodeFromJsonElement(SMessage.SubMessage.serializer(), value)
        assertEquals("hello", ssm.message)

       value =  JsonObject(mapOf("message" to JsonPrimitive("hello")))
        val ssm1 = jsonDefault.decodeFromJsonElement(SMessage.Companion, value)
        assert(ssm1 is SMessage.SubMessage)
        assertEquals("hello", (ssm1 as SMessage.SubMessage).message)


      value =   JsonObject(mapOf("smessage" to JsonObject(mapOf("message" to JsonPrimitive("hello3"))))
        )
        val sw = jsonDefault.decodeFromJsonElement(SWrapper.serializer(),
                value)
        assert(sw.smessage is SMessage.SubMessage)
        assertEquals("hello3", (sw.smessage as SMessage.SubMessage).message)

       value =  JsonObject(mapOf("body" to JsonPrimitive("msg01")))
        val t = jsonDefault.decodeFromJsonElement(TextMessage.serializer(), value)
        assertEquals("msg01", t.body)
        assertEquals("m.text", t.msgtype)
        assertNull(t.formatted_body)
        val o = jsonDefault.decodeFromString(M_Message.serializer(), """{"body":"b2","msgtype":null}""")
        assertEquals("\"b2\"", (o as UnrecognizedMessage).raw["body"].toString())
        assertEquals(JsonNull, (o as UnrecognizedMessage).raw["msgtype"])
        assertEquals("b2", o.body)
        val n = jsonDefault.decodeFromString(M_Message.serializer(), """{"body":"b3","msgtype":"m.notice"}""")
        n as NoticeMessage
        assertEquals("m.notice", n.msgtype)
        assertEquals("b3", n.body)
    }
    @Test
    fun test1() {
        val text = TextMessage("msg1")
        val textJ = jsonDefault.encodeToString(TextMessage.serializer(), text)
        assertEquals("""{"body":"msg1","formatted_body":null,"msgtype":"m.text","format":null}""", textJ)
        val tm = Properties.storeNullable(TextMessage.serializer(), text)
        assertEquals("msg1", tm["body"])
        assertNull(tm["formatted_body"])
        assertEquals("m.text", tm["msgtype"])
        assertNull(tm["format"])
    }

    @Test
    fun test2() {
        val Emote = EmoteMessage("msg1")
        val EmoteJ = jsonDefault.encodeToString(EmoteMessage.serializer(), Emote)
        assertEquals("""{"body":"msg1","msgtype":"m.emote"}""", EmoteJ)
        val tm = Properties.storeNullable(EmoteMessage.serializer(), Emote)
        assertEquals("msg1", tm["body"])
        assertNull(tm["formatted_body"])
        assertEquals("m.emote", tm["msgtype"])
        assertNull(tm["format"])
    }

    @Test
    fun testPoly() {
        val messageModule = SerializersModule {
            polymorphic(M_Message::class, EmoteMessage::class, EmoteMessage.serializer())
            polymorphic(M_Message::class, TextMessage::class, TextMessage.serializer())
        }
        val json = Json {
            classDiscriminator = "testingType"
            serializersModule = messageModule
        }
        val serializer = PolymorphicSerializer(M_Message::class)
        val text1 = json.encodeToString(serializer, TextMessage("msg1"))
        assertEquals("""{"testingType":"m.text","body":"msg1","formatted_body":null,"msgtype":"m.text","format":null}""", text1)
        val mapper = Properties(messageModule)
        val text2 = mapper.storeNullable(serializer, TextMessage("msg2"))
        assertEquals("m.text", text2["type"]) { "map $text2"}
        assertEquals("msg2", text2["value.body"])
        assertNull(text2["value.formatted_body"])
        assertEquals("m.text", text2["value.msgtype"])
        assertNull(text2["value.format"])
        assertEquals(5, text2.size)
    }
    @Test
    fun testWithoutSerialModule() {
        val json = Json {
            classDiscriminator = "testingType"
        }
        val wrapped = MessageWrapper(TextMessage("msg-1"))
        val w0 = json.encodeToString(MessageWrapper.serializer(), wrapped)
        assertEquals("""{"m":{"body":"msg-1","formatted_body":null,"msgtype":"m.text","format":null}}""", w0)
        val t0 = json.encodeToString(M_Message.serializer(), TextMessage("msg0"))
        assertEquals("""{"body":"msg0","formatted_body":null,"msgtype":"m.text","format":null}""", t0)

        val e1 = json.encodeToString(M_Message.serializer(), EmoteMessage("msg0"))
        assertEquals("""{"body":"msg0","msgtype":"m.emote"}""",
                e1)

        val mapper = Properties()
        assertThrows<SerializationException> {
           mapper.store(M_Message.serializer(), EmoteMessage("emote2"))
        }

        assertThrows<SerializationException> {
            mapper.storeNullable(M_Message.serializer(), TextMessage("msg2"))
        }

        val emoteMessageMap = Properties.store(EmoteMessage.serializer(), EmoteMessage("emotemsg"))
        assertEquals("emotemsg", emoteMessageMap["body"])
        assertEquals("m.emote", emoteMessageMap["msgtype"])
        assertEquals(2, emoteMessageMap.size)
    }
}

@Serializable
private data class MessageWrapper(val m: M_Message)
