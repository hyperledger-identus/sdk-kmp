package io.iohk.atala.prism.walletsdk.prismagent

import io.iohk.atala.prism.walletsdk.domain.models.AttachmentDescriptor
import io.iohk.atala.prism.walletsdk.domain.models.DID
import io.iohk.atala.prism.walletsdk.domain.models.Message

fun Message.Companion.testable(
    id: String = "test1",
    piuri: String = "test",
    from: DID? = null,
    to: DID? = null,
    fromPrior: String? = null,
    body: String = "",
    extraHeaders: Map<String, String> = emptyMap(),
    createdTime: String = "",
    expiresTimePlus: String = "",
    attachments: Array<AttachmentDescriptor> = emptyArray(),
    thid: String? = null,
    pthid: String? = null,
    ack: Array<String> = emptyArray(),
    direction: Message.Direction = Message.Direction.RECEIVED
): Message {
    return Message(
        id,
        piuri,
        from,
        to,
        fromPrior,
        body,
        extraHeaders,
        createdTime,
        expiresTimePlus,
        attachments,
        thid,
        pthid,
        ack,
        direction
    )
}
