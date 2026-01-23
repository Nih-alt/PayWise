package com.nihal.paywise.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.nihal.paywise.data.local.entity.AttachmentEntity
import java.io.File

@Composable
fun AttachmentSection(
    attachments: List<AttachmentEntity>,
    filesDir: File,
    onAddClick: () -> Unit,
    onRemoveClick: (AttachmentEntity) -> Unit,
    onAttachmentClick: (AttachmentEntity) -> Unit
) {
    GlassCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ReceiptLong, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Receipts", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                
                TextButton(onClick = onAddClick) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add")
                }
            }

            if (attachments.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No receipts attached",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(attachments) { attachment ->
                        AttachmentThumbnail(
                            attachment = attachment,
                            filesDir = filesDir,
                            onRemove = { onRemoveClick(attachment) },
                            onClick = { onAttachmentClick(attachment) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AttachmentThumbnail(
    attachment: AttachmentEntity,
    filesDir: File,
    onRemove: () -> Unit,
    onClick: () -> Unit
) {
    val file = File(filesDir, attachment.storedRelativePath)
    val isPdf = attachment.mimeType == "application/pdf"

    Box(modifier = Modifier.size(80.dp)) {
        Surface(
            modifier = Modifier.fillMaxSize().clickable { onClick() },
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        ) {
            if (isPdf) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Description, null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                }
            } else {
                AsyncImage(
                    model = file,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Surface(
            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(20.dp),
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.6f),
            onClick = onRemove
        ) {
            Icon(Icons.Default.Close, null, modifier = Modifier.padding(4.dp), tint = Color.White)
        }
    }
}
