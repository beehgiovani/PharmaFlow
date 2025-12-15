package com.developersbeeh.pharmaflow.features.cart

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.developersbeeh.pharmaflow.utils.PixUtils

@Composable
fun PixPaymentDialog(
    pixKey: String,
    amount: Double,
    storeName: String,
    onDismiss: () -> Unit,
    onConfirmPaid: () -> Unit
) {
    val context = LocalContext.current
    // Gera o código Pix "Copia e Cola"
    val pixPayload = remember(pixKey, amount) {
        PixUtils.generatePixPayload(pixKey, amount, storeName)
    }
    // Gera a imagem do QR Code
    val qrBitmap = remember(pixPayload) {
        PixUtils.generateQRCodeBitmap(pixPayload)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.QrCode2,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Pagamento via Pix",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Valor: R$ ${String.format("%.2f", amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Área do QR Code
                if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "QR Code Pix",
                        modifier = Modifier
                            .size(200.dp)
                            .background(Color.White)
                            .padding(8.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .background(Color.LightGray),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Erro ao gerar QR")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Botão Copiar Código
                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Código Pix", pixPayload)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Código Pix copiado!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ContentCopy, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Copiar Código Pix")
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Botão Já Paguei
                Button(
                    onClick = onConfirmPaid,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                ) {
                    Icon(Icons.Default.Verified, null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("JÁ PAGUEI / ENVIAR COMPROVANTE", color = Color.White)
                }

                Spacer(modifier = Modifier.height(8.dp))

                androidx.compose.material3.TextButton(onClick = onDismiss) {
                    Text("Cancelar / Mudar Pagamento", color = Color.Gray)
                }
            }
        }
    }
}