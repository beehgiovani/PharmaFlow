package com.developersbeeh.pharmaflow.utils

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import com.developersbeeh.pharmaflow.data.model.Converters
import com.developersbeeh.pharmaflow.data.model.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object PrinterHelper {

    // UUID Padrão para impressoras Bluetooth (Serial Port Profile)
    private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    // --- COMANDOS ESC/POS (Linguagem da Impressora) ---
    private val CMD_RESET = byteArrayOf(0x1B, 0x40)
    private val CMD_ALIGN_LEFT = byteArrayOf(0x1B, 0x61, 0x00)
    private val CMD_ALIGN_CENTER = byteArrayOf(0x1B, 0x61, 0x01)
    private val CMD_BOLD_ON = byteArrayOf(0x1B, 0x45, 0x01)
    private val CMD_BOLD_OFF = byteArrayOf(0x1B, 0x45, 0x00)
    private val CMD_TEXT_NORMAL = byteArrayOf(0x1D, 0x21, 0x00)
    private val CMD_TEXT_DOUBLE = byteArrayOf(0x1D, 0x21, 0x11)
    private val CMD_TEXT_LARGE = byteArrayOf(0x1D, 0x21, 0x01)
    private val CMD_FEED_3 = byteArrayOf(0x1B, 0x64, 0x03)

    @SuppressLint("MissingPermission") // Checagem manual feita dentro do código
    suspend fun printOrder(context: Context, order: Order, device: BluetoothDevice): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            var socket: BluetoothSocket? = null
            try {
                // Verificação de segurança Android 12+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        return@withContext Result.failure(Exception("Permissão Bluetooth não concedida."))
                    }
                }

                // Tenta conectar
                socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                socket.connect()

                val outputStream = socket.outputStream

                // --- INICIO DA IMPRESSÃO DO CUPOM ---

                // Cabeçalho
                outputStream.write(CMD_RESET)
                outputStream.write(CMD_ALIGN_CENTER)
                outputStream.write(CMD_BOLD_ON)
                outputStream.write(CMD_TEXT_DOUBLE)
                outputStream.write("PHARMA FLOW\n".toByteArray())
                outputStream.write(CMD_TEXT_NORMAL)
                outputStream.write(CMD_BOLD_OFF)
                outputStream.write("${order.storeName}\n".toByteArray())
                outputStream.write("================================\n".toByteArray())

                // Dados Pedido
                outputStream.write(CMD_ALIGN_LEFT)
                outputStream.write(CMD_TEXT_LARGE)
                outputStream.write("PEDIDO: #${order.firestoreId.takeLast(5).uppercase()}\n".toByteArray())
                outputStream.write(CMD_TEXT_NORMAL)

                val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(order.date))
                outputStream.write("Data: $dateStr\n".toByteArray())

                // Cliente
                outputStream.write("--------------------------------\n".toByteArray())
                outputStream.write("CLIENTE:\n".toByteArray())
                outputStream.write(CMD_BOLD_ON)
                outputStream.write("${order.customerName}\n".toByteArray())
                outputStream.write(CMD_BOLD_OFF)
                if (order.customerPhone.isNotEmpty()) outputStream.write("Tel: ${order.customerPhone}\n".toByteArray())

                // Entrega
                outputStream.write("--------------------------------\n".toByteArray())
                if (order.isDelivery) {
                    outputStream.write(CMD_BOLD_ON)
                    outputStream.write("ENTREGA:\n".toByteArray())
                    outputStream.write(CMD_BOLD_OFF)
                    outputStream.write("${order.deliveryAddress}\n".toByteArray())
                } else {
                    outputStream.write(CMD_BOLD_ON)
                    outputStream.write("RETIRADA NA LOJA\n".toByteArray())
                    outputStream.write(CMD_BOLD_OFF)
                }

                // Itens
                outputStream.write("================================\n".toByteArray())
                outputStream.write("ITENS:\n".toByteArray())
                outputStream.write(CMD_BOLD_ON)

                val converter = Converters()
                val items = try { converter.fromString(order.itemsJson) } catch (e: Exception) { emptyList() }

                items.forEach { item ->
                    // Formato: 2x NOME DO PRODUTO ....... R$ 99.99
                    val qtd = "${item.quantity}x "
                    outputStream.write(qtd.toByteArray())
                    outputStream.write("${item.name}\n".toByteArray())

                    val priceStr = "   Vl. Un: R$ ${String.format("%.2f", item.price)}\n"
                    outputStream.write(priceStr.toByteArray())
                }
                outputStream.write(CMD_BOLD_OFF)

                // Totais
                outputStream.write("--------------------------------\n".toByteArray())
                outputStream.write(CMD_ALIGN_LEFT)

                if (order.discountAmount > 0) {
                    outputStream.write("Desconto: - R$ ${String.format("%.2f", order.discountAmount)}\n".toByteArray())
                }
                if (order.deliveryFee > 0) {
                    outputStream.write("Entrega:  + R$ ${String.format("%.2f", order.deliveryFee)}\n".toByteArray())
                }

                outputStream.write(CMD_BOLD_ON)
                outputStream.write(CMD_TEXT_DOUBLE)
                outputStream.write("TOTAL: R$ ${String.format("%.2f", order.totalAmount)}\n".toByteArray())
                outputStream.write(CMD_TEXT_NORMAL)
                outputStream.write(CMD_BOLD_OFF)

                outputStream.write("--------------------------------\n".toByteArray())

                // Dados de Pagamento (Com Obs e Troco)
                // Faz parse simples do campo de texto
                val pgtoClean = order.paymentMethod.replace("|", "\n").replace("Obs:", "\nObs:")
                outputStream.write("Pagamento:\n$pgtoClean\n".toByteArray())

                // Rodapé
                outputStream.write(CMD_ALIGN_CENTER)
                outputStream.write("\n\n".toByteArray())
                outputStream.write("Obrigado pela preferencia!\n".toByteArray())
                outputStream.write("www.pharmaflow.com.br\n".toByteArray())
                outputStream.write(CMD_FEED_3) // Pula linha para destacar

                outputStream.flush()
                Thread.sleep(1000) // Delay para buffer da impressora esvaziar

                Result.success(true)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            } finally {
                try { socket?.close() } catch (e: Exception) {}
            }
        }
    }
}