package io.kanro.mediator.desktop.ui

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.bybutter.sisyphus.jackson.toJson
import com.bybutter.sisyphus.protobuf.ProtoReflection
import com.bybutter.sisyphus.protobuf.invoke
import com.bybutter.sisyphus.protobuf.primitives.FieldDescriptorProto
import com.bybutter.sisyphus.protobuf.primitives.string
import com.bybutter.sisyphus.rpc.Code
import com.bybutter.sisyphus.rpc.Status
import com.bybutter.sisyphus.string.toUpperSpaceCase
import io.grpc.Metadata
import io.kanro.compose.jetbrains.JBTheme
import io.kanro.compose.jetbrains.SelectionScope
import io.kanro.compose.jetbrains.control.Icon
import io.kanro.compose.jetbrains.control.JBTreeItem
import io.kanro.compose.jetbrains.control.JBTreeList
import io.kanro.compose.jetbrains.control.JPanelBorder
import io.kanro.compose.jetbrains.control.ListItemHoverIndication
import io.kanro.compose.jetbrains.control.ProgressBar
import io.kanro.compose.jetbrains.control.Tab
import io.kanro.compose.jetbrains.control.Text
import io.kanro.compose.jetbrains.interaction.hoverable
import io.kanro.mediator.desktop.LocalMainViewModel
import io.kanro.mediator.desktop.LocalWindow
import io.kanro.mediator.desktop.model.CallEvent
import io.kanro.mediator.desktop.model.CallTimeline
import io.kanro.mediator.desktop.model.asState
import io.kanro.mediator.utils.toMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Cursor
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CallView(call: CallTimeline?) {
    var selectedTab by remember { mutableStateOf(0) }
    var height by remember { mutableStateOf(200.dp) }

    Box(Modifier.fillMaxWidth()) {
        JPanelBorder(Modifier.fillMaxWidth().height(1.dp).align(Alignment.BottomStart))

        Box(
            Modifier.matchParentSize()
                .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR)))
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { delta ->
                        height -= delta.dp
                    }
                )
        ) {}
        Row(Modifier.fillMaxWidth().align(Alignment.CenterStart)) {
            Tab(
                selectedTab == 0,
                {
                    selectedTab = 0
                }
            ) {
                Row(
                    modifier = Modifier.padding(7.dp, 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon("icons/ppLib.svg")
                    Text("Statistics", modifier = Modifier.offset(y = (-1).dp))
                }
            }
            Tab(
                selectedTab == 1,
                {
                    selectedTab = 1
                }
            ) {
                Row(
                    modifier = Modifier.padding(7.dp, 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon("icons/timeline.svg")
                    Text("Timeline", modifier = Modifier.offset(y = (-1).dp))
                }
            }
        }
    }

    Box(Modifier.fillMaxWidth().height(height)) {
        if (call == null) {
            Text("Select one call to view details", Modifier.align(Alignment.Center))
        } else {
            when (selectedTab) {
                0 -> StatisticsView(call)
                1 -> TimelineView(call)
            }
        }
    }
}

@Composable
fun StatisticsView(call: CallTimeline) {
    Column(Modifier.background(Color.White).fillMaxSize()) {

        var selectedKey by remember(call) { mutableStateOf(-1) }

        MetadataItem("ID", call.id, selectedKey == 0) {
            selectedKey = 0
        }
        val start = call.start()
        MetadataItem("Authority", start.authority, selectedKey == 1) {
            selectedKey = 1
        }
        MetadataItem("Method", start.method, selectedKey == 2) {
            selectedKey = 2
        }
        MetadataItem("Start time", start.timestamp().string(), selectedKey == 3) {
            selectedKey = 3
        }
        val close = call.close()
        if (close != null) {
            MetadataItem("End time", close.timestamp().string(), selectedKey == 4) {
                selectedKey = 4
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TimelineView(call: CallTimeline) {
    var selectedEvent by remember(call) { mutableStateOf<CallEvent?>(null) }
    var width by remember { mutableStateOf(200.dp) }

    Row {
        val vState = rememberScrollState()
        Box {
            Column(Modifier.width(width).verticalScroll(vState)) {
                call.events().forEach {
                    TimelineItemRow(call, it, selectedEvent == it) {
                        selectedEvent = it
                    }
                }
            }

            VerticalScrollbar(rememberScrollbarAdapter(vState), Modifier.align(Alignment.CenterEnd))
        }

        JPanelBorder(
            Modifier.width(1.dp).fillMaxHeight()
                .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR)))
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        width += delta.dp
                    }
                )
        )

        Box {
            val timeline by call.asState()
            if (selectedEvent == null) {
                Text("Select one event to view details", Modifier.align(Alignment.Center))
            } else {
                when (val event = selectedEvent) {
                    is CallEvent.Accept -> EventView(event)
                    is CallEvent.Close -> EventView(event)
                    is CallEvent.Input -> EventView(timeline, event)
                    is CallEvent.Output -> EventView(timeline, event)
                    is CallEvent.Start -> EventView(event)
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun TimelineItemRow(
    timeline: CallTimeline,
    event: CallEvent,
    selected: Boolean,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    role: Role? = null,
    onClick: () -> Unit
) {
    ContextMenuArea({
        val result = mutableListOf<ContextMenuItem>()
        when (event) {
            is CallEvent.Start -> {
                result += ContextMenuItem("Copy Authority") {
                    Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(event.authority), null)
                }
                result += ContextMenuItem("Copy Method Name") {
                    Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(event.method), null)
                }
                result += ContextMenuItem("Copy Headers") {
                    Toolkit.getDefaultToolkit().systemClipboard.setContents(
                        StringSelection(
                            event.header.toMap().toJson()
                        ),
                        null
                    )
                }
            }
            is CallEvent.Accept -> {
                result += ContextMenuItem("Copy Headers") {
                    Toolkit.getDefaultToolkit().systemClipboard.setContents(
                        StringSelection(
                            event.header.toMap().toJson()
                        ),
                        null
                    )
                }
            }
            is CallEvent.Close -> {
                result += ContextMenuItem("Copy Trails") {
                    Toolkit.getDefaultToolkit().systemClipboard.setContents(
                        StringSelection(
                            event.trails.toMap().toJson()
                        ),
                        null
                    )
                }
            }
            is CallEvent.Input -> {
                val json = timeline.reflection().invoke {
                    event.message().toJson()
                }
                result += ContextMenuItem("Copy Message") {
                    Toolkit.getDefaultToolkit().systemClipboard.setContents(
                        StringSelection(json), null
                    )
                }
            }
            is CallEvent.Output -> {
                val json = timeline.reflection().invoke {
                    event.message().toJson()
                }
                result += ContextMenuItem("Copy Message") {
                    Toolkit.getDefaultToolkit().systemClipboard.setContents(
                        StringSelection(json), null
                    )
                }
            }
        }
        result
    }) {
        SelectionScope(selected) {
            Row(
                modifier = Modifier.height(23.dp).fillMaxWidth().selectable(
                    selected = selected,
                    interactionSource = interactionSource,
                    indication = ListItemHoverIndication,
                    onClick = onClick,
                    role = role
                ).run {
                    if (selected) {
                        background(color = JBTheme.selectionColors.active)
                    } else {
                        this
                    }
                }.hoverable(rememberCoroutineScope(), interactionSource),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val icon = when (event) {
                    is CallEvent.Accept -> "icons/reviewAccepted.svg"
                    is CallEvent.Close -> "icons/reviewRejected.svg"
                    is CallEvent.Input -> "icons/showWriteAccess.svg"
                    is CallEvent.Output -> "icons/showReadAccess.svg"
                    is CallEvent.Start -> "icons/connector.svg"
                }
                val text = when (event) {
                    is CallEvent.Accept -> "Server Accepted"
                    is CallEvent.Close -> "Closed"
                    is CallEvent.Input -> "Client Messaging"
                    is CallEvent.Output -> "Server Messaging"
                    is CallEvent.Start -> "Start"
                }

                Icon(icon, modifier = Modifier.padding(7.dp))
                Text(text, maxLines = 1)
            }
        }
    }
}

@Composable
fun EventView(event: CallEvent.Start) {
    Column {
        MetadataView(event.header)
    }
}

@Composable
fun EventView(event: CallEvent.Accept) {
    Column {
        MetadataView(event.header)
    }
}

@Composable
fun EventView(event: CallEvent.Close) {
    Column {
        MetadataView(event.trails)
    }
}

@Composable
fun EventView(call: CallTimeline, event: CallEvent) {
    var resolveFailed by remember { mutableStateOf(false) }

    val serverManager = LocalMainViewModel.current.serverManager
    if (!event.resolved() && serverManager != null) {
        LaunchedEffect(call) {
            if (event.resolved()) return@LaunchedEffect
            val ref = serverManager.reflection(call.start().authority)
            withContext(Dispatchers.IO) {
                ref.collect()
            }
            if (ref.resolved()) {
                call.resolve(ref)
            } else {
                resolveFailed = true
            }
        }
    }

    if (event.resolved()) {
        when (event) {
            is CallEvent.Input -> MessageView(call.reflection(), event.message())
            is CallEvent.Output -> MessageView(call.reflection(), event.message())
        }
    } else {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column {
                when {
                    resolveFailed -> {
                        Text("Fail to resolve call")
                    }
                    serverManager != null -> {
                        ProgressBar()
                        Text("Resolving...")
                    }
                    else -> {
                        Text("Need start proxy server to resolve calls")
                    }
                }
            }
        }
    }
}

@Composable
fun MetadataView(metadata: Metadata, modifier: Modifier = Modifier) {
    Box(Modifier.background(JBTheme.panelColors.bgContent).fillMaxSize()) {
        val vState = rememberScrollState()
        JBTreeList(modifier.verticalScroll(vState)) {
            val selectedKey = remember(metadata) { mutableStateOf("") }

            metadata.keys().forEach {
                val key = it.lowercase()
                if (key.endsWith("-bin")) {
                    val value = metadata[Metadata.Key.of(it, Metadata.BINARY_BYTE_MARSHALLER)] ?: byteArrayOf()
                    MessageFieldView(
                        Modifier, selectedKey, ProtoReflection.current(),
                        FieldDescriptorProto {
                            this.type = FieldDescriptorProto.Type.MESSAGE
                            this.name = "status"
                            this.typeName = Status.name
                        },
                        it, Status.parse(value)
                    )
                } else {
                    val value = metadata[Metadata.Key.of(it, Metadata.ASCII_STRING_MARSHALLER)] ?: ""
                    MetadataItem(
                        it, value, selectedKey.value == it
                    ) {
                        selectedKey.value = it
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MetadataItem(
    key: String,
    value: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    JBTreeItem(modifier = Modifier.fillMaxWidth(), selected = selected, onClick = onClick) {
        ContextMenuArea({
            val result = mutableListOf<ContextMenuItem>()
            result += ContextMenuItem("Copy") {
                Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection("$key: $value"), null)
            }
            result += ContextMenuItem("Copy Key") {
                Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(key), null)
            }
            result += ContextMenuItem("Copy Value") {
                Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(value), null)
            }
            result
        }) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Label(key)
                Label(" = ", color = JBTheme.textColors.infoInput)
                Label(value)
                if (key.lowercase() == "grpc-status") {
                    val code = Code.fromNumber(value.toInt())
                    if (code != null) {
                        Label(" (${code.name.toUpperSpaceCase()})", color = JBTheme.textColors.infoInput)
                    }
                }
            }
        }
    }
}
