package koma.matrix.event.room_message.chat

import koma.matrix.json.jsonDefault
import koma.matrix.json.jsonDefaultConf
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.SerializersModule
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows

internal class TextMessageTest {

    @Test
    fun testOtherMessage() {
        val text = UnrecognizedMessage(JsonObject(mapOf("a" to JsonPrimitive("b"), "c" to JsonPrimitive("d)"))))
        val textJ = jsonDefault.stringify(M_Message.serializer(), text)
        assertEquals("""{"a":"b","c":"d)"}""", textJ)
        val unk = jsonDefault.parse(M_Message.serializer(), """{"a":"b","body":"d"}""")
        assertEquals("\"b\"", (unk as UnrecognizedMessage).raw["a"].toString())
        assertEquals("\"d\"", unk.raw["body"].toString())
        jsonDefault.parse(M_Message.serializer(), """{"body":"b1","msgtype":"m.!"}""")
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
                    PrimitiveDescriptor("SMessage", PrimitiveKind.STRING)

            override fun serialize(encoder: Encoder, obj: SMessage) {
                val output = encoder as? JsonOutput ?: throw SerializationException("This class can be saved only by Json, not $encoder")
                val tree = when (obj) {
                    is SubMessage -> output.json.toJson(SubMessage.serializer(), obj)
                }
                output.encodeJson(tree)
            }

            override fun deserialize(decoder: Decoder): SMessage {
                val input = decoder as? JsonInput ?: throw SerializationException("This class can be loaded only by Json")
                val tree = input.decodeJson() as? JsonObject ?: throw SerializationException("Expected JsonObject")
                return input.json.fromJson(SubMessage.serializer(), tree)
            }
        }
    }

    @Serializable
    class SWrapper(val smessage: SMessage)

    @Test
    fun testDeserialize() {
        val tt = jsonDefault.fromJson(TestingMessage.serializer(), JsonObject(mapOf("body" to JsonPrimitive("msg0t"))))
        assertEquals("msg0t", tt.body)
        assertEquals("m.testing", tt.msgtype)
        assertNull(tt.format)

        val tsub = jsonDefault.fromJson(BasicMessage.SubMessage.serializer(), JsonObject(mapOf("message" to JsonPrimitive("hello"))))
        assertEquals("hello", tsub.message)

        val ssm = jsonDefault.fromJson(SMessage.SubMessage.serializer(), JsonObject(mapOf("message" to JsonPrimitive("hello"))))
        assertEquals("hello", ssm.message)

        val ssm1 = jsonDefault.fromJson(SMessage.Companion, JsonObject(mapOf("message" to JsonPrimitive("hello"))))
        assert(ssm1 is SMessage.SubMessage)
        assertEquals("hello", (ssm1 as SMessage.SubMessage).message)

        val sw = jsonDefault.fromJson(SWrapper.serializer(),
                JsonObject(mapOf("smessage" to JsonObject(mapOf("message" to JsonPrimitive("hello3"))))
                ))
        assert(sw.smessage is SMessage.SubMessage)
        assertEquals("hello3", (sw.smessage as SMessage.SubMessage).message)

        val t = jsonDefault.fromJson(TextMessage.serializer(), JsonObject(mapOf("body" to JsonPrimitive("msg01"))))
        assertEquals("msg01", t.body)
        assertEquals("m.text", t.msgtype)
        assertNull(t.formatted_body)
        val o = jsonDefault.parse(M_Message.serializer(), """{"body":"b2","msgtype":null}""")
        assertEquals("\"b2\"", (o as UnrecognizedMessage).raw["body"].toString())
        assertEquals(JsonNull, (o as UnrecognizedMessage).raw["msgtype"])
        assertEquals("b2", o.body)
        val n = jsonDefault.parse(M_Message.serializer(), """{"body":"b3","msgtype":"m.notice"}""")
        n as NoticeMessage
        assertEquals("m.notice", n.msgtype)
        assertEquals("b3", n.body)
    }
    @Test
    fun test1() {
        val text = TextMessage("msg1")
        val textJ = jsonDefault.stringify(TextMessage.serializer(), text)
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
        val EmoteJ = jsonDefault.stringify(EmoteMessage.serializer(), Emote)
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
            polymorphic(M_Message::class) {
                TextMessage::class with TextMessage.serializer()
                EmoteMessage::class with EmoteMessage.serializer()
            }
        }
        val json = Json(jsonDefaultConf.copy(classDiscriminator = "testingType"), context = messageModule)
        val serializer = PolymorphicSerializer(M_Message::class)
        val text1 = json.stringify(serializer, TextMessage("msg1"))
        assertEquals("""{"testingType":"m.text","body":"msg1","formatted_body":null,"msgtype":"m.text","format":null}""", text1)
        val mapper = Properties(messageModule)
        val text2 = mapper.storeNullable(serializer, TextMessage("msg2"))
        assertEquals("m.text", text2["class"])
        assertEquals("msg2", text2["value.body"])
        assertNull(text2["value.formatted_body"])
        assertEquals("m.text", text2["value.msgtype"])
        assertNull(text2["value.format"])
        assertEquals(5, text2.size)
    }
    @Test
    fun testWithoutSerialModule() {
        val json = Json(jsonDefaultConf.copy(classDiscriminator = "testingType"))
        val wrapped = MessageWrapper(TextMessage("msg-1"))
        val w0 = json.stringify(MessageWrapper.serializer(), wrapped)
        assertEquals("""{"m":{"body":"msg-1","formatted_body":null,"msgtype":"m.text","format":null}}""", w0)
        val t0 = json.stringify(M_Message.serializer(), TextMessage("msg0"))
        assertEquals("""{"body":"msg0","formatted_body":null,"msgtype":"m.text","format":null}""", t0)

        val e1 = json.stringify(M_Message.serializer(), EmoteMessage("msg0"))
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
