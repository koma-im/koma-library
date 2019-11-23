package koma.matrix.publicapi.rooms

import kotlinx.serialization.Serializable

@Serializable
class RoomDirectoryQuery(
        val filter: RoomDirectoryFilter,
        val since: String? = null,
        /**
         * for example
         * irc-freenode|freenode
         * gitter|gitter
         */
        val third_party_instance_id: String? = null,
        /**
         * the default of synapse seems to be 100
         */
        val limit: Int = 20
)

@Serializable
class RoomDirectoryFilter(
        val generic_search_term: String
)
