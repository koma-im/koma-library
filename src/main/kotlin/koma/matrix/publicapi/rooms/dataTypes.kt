package koma.matrix.publicapi.rooms

import kotlinx.serialization.Serializable

@Serializable
class RoomDirectoryQuery(
        val filter: RoomDirectoryFilter? = null,
        val since: String? = null,
        /**
         * The specific third party network/protocol to request from the homeserver.
         * Can only be used if include_all_networks is false.
         * for example
         * irc-freenode|freenode
         * gitter|gitter
         */
        val third_party_instance_id: String? = null,
        /**
         * Whether or not to include all known networks/protocols from
         * application services on the homeserver. Defaults to false
         */
        val include_all_networks: Boolean = false,
        /**
         * the default of synapse seems to be 100
         */
        val limit: Int
)

@Serializable
class RoomDirectoryFilter(
        val generic_search_term: String
)
