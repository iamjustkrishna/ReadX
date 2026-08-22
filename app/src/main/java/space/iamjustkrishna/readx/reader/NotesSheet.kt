package space.iamjustkrishna.readx.reader

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import space.iamjustkrishna.readx.model.Highlight
import space.iamjustkrishna.readx.ui.theme.MinimalistLightColors
import space.iamjustkrishna.readx.ui.theme.ThemeColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesSheet(
    viewModel: ReaderViewModel,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onSelectHighlight: (Highlight) -> Unit = {},
    themeColors: ThemeColors = MinimalistLightColors
) {
    val highlights by viewModel.highlights.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = themeColors.surface,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = themeColors.onSurfaceVariant.copy(alpha = 0.4f))
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Highlights & Notes ()",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = themeColors.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (highlights.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No highlights yet. Select text in the document and tap 'Highlight' to save highlights and personal notes here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = themeColors.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    items(highlights, key = { it.id }) { highlight ->
                        HighlightItem(
                            highlight = highlight,
                            onClick = { onSelectHighlight(highlight) },
                            onDelete = { viewModel.deleteHighlight(highlight.id) },
                            onUpdateNote = { note -> viewModel.updateHighlightNote(highlight.id, note) },
                            themeColors = themeColors
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HighlightItem(
    highlight: Highlight,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onUpdateNote: (String) -> Unit,
    themeColors: ThemeColors
) {
    var editingNote by remember { mutableStateOf(false) }
    var noteText by remember(highlight.note) { mutableStateOf(highlight.note) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = themeColors.surfaceVariant),
        border = BorderStroke(1.dp, themeColors.cardBorder),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(highlight.color))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Page ",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = themeColors.primary
                    )
                }

                Row {
                    IconButton(
                        onClick = { editingNote = !editingNote },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (editingNote) Icons.Default.Check else Icons.Default.Edit,
                            contentDescription = "Edit note",
                            tint = themeColors.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete highlight",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "\"\"",
                style = MaterialTheme.typography.bodyMedium,
                color = themeColors.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            if (highlight.note.isNotEmpty() || editingNote) {
                Spacer(modifier = Modifier.height(8.dp))
                if (editingNote) {
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = {
                            noteText = it
                            onUpdateNote(it)
                        },
                        label = { Text("Personal Note", color = themeColors.onSurfaceVariant) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = themeColors.surface,
                            unfocusedContainerColor = themeColors.surface,
                            focusedTextColor = themeColors.onSurface,
                            unfocusedTextColor = themeColors.onSurface,
                            focusedBorderColor = themeColors.primary,
                            unfocusedBorderColor = themeColors.cardBorder
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 3
                    )
                } else {
                    Text(
                        text = "Note: ",
                        style = MaterialTheme.typography.bodySmall,
                        color = themeColors.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}