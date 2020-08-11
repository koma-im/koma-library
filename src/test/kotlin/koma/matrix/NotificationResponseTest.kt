package koma.matrix

import koma.matrix.json.jsonDefault
import kotlinx.serialization.json.content
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals

internal class NotificationResponseTest {
    @Test
    fun deserializeNotificationResponse() {
        val notification1 = jsonDefault.decodeFromString(NotificationResponse.serializer(), example1)
        assertEquals("abcdef", notification1.next_token)
        assertEquals(1, notification1.notifications.size)
        val notif1 = notification1.notifications[0]
        assertEquals(1, notif1.actions.size)
        val action = notif1.actions[0]
        assertEquals("notify", action.jsonPrimitive.content)
        assertEquals("hcbvkzxhcvb", notif1.profile_tag)
        assert(notif1.read)
        assertEquals(1475508881945, notif1.ts)
        assertEquals("""$143273582443PhrSn:example.org""", notif1.event.event_id)

        val notifRes2 = jsonDefault.decodeFromString(NotificationResponse.serializer(), exampl2)
        assertEquals("1000116789", notifRes2.next_token)
    }

    @Test
    fun deserializeNotification() {
        jsonDefault.decodeFromString(NotificationResponse.Notification.serializer(), notificationString1)
        val notif2 = jsonDefault.decodeFromString(NotificationResponse.Notification.serializer(), notificationString2)
        assertEquals("!room2:example.com", notif2.room_id.full)
        val event = notif2.event
        assertEquals( "$156141234550951DWylX:example.com", notif2.event.event_id)
        val unsigned = event.unsigned
        assertEquals(51106879, unsigned?.age)
        val prevEvent = unsigned?.prev_content
        assert(prevEvent is NotificationResponse.Event)
        prevEvent as NotificationResponse.Event
        assertEquals("$123prev:example.com", prevEvent.event_id)
    }
    val notificationString2 = """
        {
              "room_id": "!room2:example.com",
              "profile_tag": null,
              "actions": [
                "notify"
              ],
              "ts": 1561412345098,
              "event": {
                "content": {
                  "body": "hi",
                  "msgtype": "m.text"
                },
                "event_id": "$156141234550951DWylX:example.com",
                "origin_server_ts": 1561412345681,
                "sender": "@ena:example.com",
                "type": "m.room.message",
                "unsigned": {
                  "age": 51106879,
                  "prev_content": {
                        "content": {
                          "body": "prevhi",
                          "msgtype": "m.text"
                        },
                        "event_id": "$123prev:example.com",
                        "origin_server_ts": 1561234,
                        "sender": "@prec:example.com",
                        "type": "m.room.message",
                        "unsigned": {
                          "age": 51
                        }
                  }
                }
              },
              "read": true
            }
    """

    val notificationString1 = """
        {
              "room_id": "!roomxyz:example.com",
              "profile_tag": null,
              "actions": [
                "notify",
                {
                  "set_tweak": "highlight",
                  "value": true
                }
              ],
              "ts": 1561412345098,
              "event": {
                "content": {
                  "body": "hi",
                  "msgtype": "m.text"
                },
                "event_id": "        ${'$'}        156141234550951DWylX:example.com",
                "origin_server_ts": 1561412345681,
                "sender": "@ena:example.com",
                "type": "m.room.message",
                "unsigned": {
                  "age": 51106879,
                  "m.relations": {
                    "m.annotation": {
                      "chunk": [
                        {
                          "type": "m.reaction",
                          "key": "\ud84d\ude80",
                          "count": 1
                        },
                        {
                          "type": "m.reaction",
                          "key": "\ud84c\uddeb\ud84c\uddee",
                          "count": 1
                        }
                      ]
                    }
                  }
                }
              },
              "read": true
            }
    """.trimIndent()
    val example1 = """
        {
  "next_token": "abcdef",
  "notifications": [
    {
      "actions": [
        "notify"
      ],
      "profile_tag": "hcbvkzxhcvb",
      "read": true,
      "room_id": "!abcdefg:example.com",
      "ts": 1475508881945,
      "event": {
        "content": {
          "body": "This is an example text message",
          "msgtype": "m.text",
          "format": "org.matrix.custom.html",
          "formatted_body": "<b>This is an example text message</b>"
        },
        "type": "m.room.message",
        "event_id": "${'$'}143273582443PhrSn:example.org",
        "room_id": "!jEsUZKDJdhlrceRyVU:example.org",
        "sender": "@example:example.org",
        "origin_server_ts": 1432735824653,
        "unsigned": {
          "age": 1234
        }
      }
    }
  ]
}
    """.trimIndent()

    val exampl2 = """
        {
  "notifications": [
    {
      "room_id": "!roomxyz:example.com",
      "profile_tag": null,
      "actions": [
        "notify",
        {
          "set_tweak": "highlight",
          "value": true
        }
      ],
      "ts": 1561412345550,
      "event": {
        "content": {
          "body": "Good morning",
          "msgtype": "m.text"
        },
        "event_id": "${'$'}1561412345467748FdohJ:example.com",
        "origin_server_ts": 1561412345104,
        "sender": "@steve:example.com",
        "type": "m.room.message",
        "unsigned": {
          "age": 90117890
        }
      },
      "read": true
    },
    {
      "room_id": "!roomxyz:example.com",
      "profile_tag": null,
      "actions": [
        "notify",
        {
          "set_tweak": "highlight",
          "value": true
        }
      ],
      "ts": 1560712345874,
      "event": {
        "content": {
          "body": "happy Friday",
          "msgtype": "m.text"
        },
        "event_id": "${'$'}1560712345944096PZaDB:example.com",
        "origin_server_ts": 1560712345815,
        "sender": "@steve:example.com",
        "type": "m.room.message",
        "unsigned": {
          "age": 689495669
        }
      },
      "read": true
    },
    {
      "room_id": "!roomxyz:example.com",
      "profile_tag": null,
      "actions": [
        "notify",
        {
          "set_tweak": "highlight",
          "value": true
        }
      ],
      "ts": 1560112345091,
      "event": {
        "content": {
          "body": "",
          "msgtype": "m.text"
        },
        "event_id": "${'$'}1560112345181714DKWZY:example.com",
        "origin_server_ts": 1560112345440,
        "sender": "@steve:example.com",
        "type": "m.room.message",
        "unsigned": {
          "age": 1161812345,
          "m.relations": {
            "m.annotation": {
              "chunk": [
                {
                  "type": "m.reaction",
                  "key": "\ud84c\udf89",
                  "count": 1
                }
              ]
            }
          }
        }
      },
      "read": true
    },
    {
      "room_id": "!roomxyz:example.com",
      "profile_tag": null,
      "actions": [
        "notify",
        {
          "set_tweak": "highlight",
          "value": true
        }
      ],
      "ts": 1560112345150,
      "event": {
        "content": {
          "body": "time to stare",
          "msgtype": "m.text"
        },
        "event_id": "${'$'}1560112345187154GEKnW:example.com",
        "origin_server_ts": 1560112345898,
        "sender": "@steve:example.com",
        "type": "m.room.message",
        "unsigned": {
          "age": 1195412345
        }
      },
      "read": true
    },
    {
      "room_id": "!roomxyz:example.com",
      "profile_tag": null,
      "actions": [
        "notify",
        {
          "set_tweak": "highlight",
          "value": true
        }
      ],
      "ts": 1569612345865,
      "event": {
        "content": {
          "body": "read this",
          "msgtype": "m.text"
        },
        "event_id": "${'$'}1569612345181448xUvpW:example.com",
        "origin_server_ts": 1569612345770,
        "sender": "@steve:example.com",
        "type": "m.room.message",
        "unsigned": {
          "age": 1864112345,
          "m.relations": {
            "m.annotation": {
              "chunk": [
                {
                  "type": "m.reaction",
                  "key": "\ud84c\udf89",
                  "count": 8
                }
              ]
            }
          }
        }
      },
      "read": true
    },
    {
      "room_id": "!roomxyz:example.com",
      "profile_tag": null,
      "actions": [
        "notify",
        {
          "set_tweak": "highlight",
          "value": true
        }
      ],
      "ts": 1569512346789,
      "event": {
        "content": {
          "body": "",
          "msgtype": "m.text"
        },
        "event_id": "${'$'}1569512345176789Rytfi:example.com",
        "origin_server_ts": 1569512346789,
        "sender": "@steve:example.com",
        "type": "m.room.message",
        "unsigned": {
          "age": 1896716789
        }
      },
      "read": true
    },
    {
      "room_id": "!roomxyz:example.com",
      "profile_tag": null,
      "actions": [
        "notify",
        {
          "set_tweak": "sound",
          "value": "default"
        },
        {
          "set_tweak": "highlight"
        }
      ],
      "ts": 1569512346789,
      "event": {
        "content": {
          "body": "thanks for sharing!",
          "format": "org.matrix.custom.html",
          "formatted_body": "",
          "msgtype": "m.text"
        },
        "event_id": "${'$'}1569512345176789QnhKA:example.com",
        "origin_server_ts": 1569512346789,
        "sender": "@steve:example.com",
        "type": "m.room.message",
        "unsigned": {
          "age": 1896816789,
          "m.relations": {
            "m.annotation": {
              "chunk": [
                {
                  "type": "m.reaction",
                  "key": "\u1764\ufe0f",
                  "count": 1
                }
              ]
            }
          }
        }
      },
      "read": true
    },
    {
      "room_id": "!roomxyz:example.com",
      "profile_tag": null,
      "actions": [
        "notify",
        {
          "set_tweak": "highlight",
          "value": true
        }
      ],
      "ts": 1568912346789,
      "event": {
        "content": {
          "body": "it's daylight here",
          "msgtype": "m.text"
        },
        "event_id": "${'$'}156891234576789mvqiW:example.com",
        "origin_server_ts": 1568912346789,
        "sender": "@steve:example.com",
        "type": "m.room.message",
        "unsigned": {
          "age": 1474616789,
          "m.relations": {
            "m.annotation": {
              "chunk": [
                {
                  "type": "m.reaction",
                  "key": "\u1600\ufe0f",
                  "count": 1
                },
                {
                  "type": "m.reaction",
                  "key": "\ud84d\ude80",
                  "count": 1
                }
              ]
            }
          }
        }
      },
      "read": true
    },
    {
      "room_id": "!roomxyz:example.com",
      "profile_tag": null,
      "actions": [
        "notify",
        {
          "set_tweak": "highlight",
          "value": true
        }
      ],
      "ts": 1568912346789,
      "event": {
        "content": {
          "body": "there will be a new edition",
          "msgtype": "m.text"
        },
        "event_id": "${'$'}1568912345416789nRpsb:example.com",
        "origin_server_ts": 1568912346789,
        "sender": "@steve:example.com",
        "type": "m.room.message",
        "unsigned": {
          "age": 1507616789,
          "m.relations": {
            "m.annotation": {
              "chunk": [
                {
                  "type": "m.reaction",
                  "key": "\ud84c\udf89",
                  "count": 1
                }
              ]
            }
          }
        }
      },
      "read": true
    },
    {
      "room_id": "!roomxyz:example.com",
      "profile_tag": null,
      "actions": [
        "notify",
        {
          "set_tweak": "highlight",
          "value": true
        }
      ],
      "ts": 1561712346789,
      "event": {
        "type": "m.room.tombstone",
        "sender": "@dave:example.com",
        "content": {
          "body": "This room has been replaced",
          "replacement_room": "!roomxyz:example.com"
        },
        "state_key": "",
        "event_id": "${'$'}156171236789XwPJT:example.com",
        "origin_server_ts": 1561712346789,
        "unsigned": {
          "age": 9757116789
        }
      },
      "read": false
    }
  ],
  "next_token": "1000116789"
}
    """.trimIndent()
}