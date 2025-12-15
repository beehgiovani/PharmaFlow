import { useEffect, useState } from 'react';
import { collection, query, orderBy, getDocs, updateDoc, doc } from 'firebase/firestore';
import { db } from '../../../lib/firebase';
import type { Order } from '../../../types';
import { 
    Clock, 
    CheckCircle, 
    Truck, 
    Package, 
    XCircle,
    MapPin,
    Phone
} from 'lucide-react';

export default function AdminOrdersPage() {
    const [orders, setOrders] = useState<Order[]>([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        fetchOrders();
    }, []);

    async function fetchOrders() {
        try {
            const q = query(collection(db, "orders"), orderBy("date", "desc"));
            const snapshot = await getDocs(q);
            const list = snapshot.docs.map(d => ({ firestoreId: d.id, ...d.data() })) as Order[];
            setOrders(list);
        } catch (error) {
            console.error("Error fetching orders:", error);
        } finally {
            setLoading(false);
        }
    }

    const handleStatusChange = async (orderId: string, newStatus: string) => {
        if (!orderId) return;
        try {
            await updateDoc(doc(db, "orders", orderId), { status: newStatus });
            setOrders(orders.map(o => o.firestoreId === orderId ? { ...o, status: newStatus } : o));
        } catch (error) {
            console.error("Error updating status:", error);
            alert("Erro ao atualizar status.");
        }
    };

    const getStatusColor = (status: string) => {
        switch (status) {
            case 'Pendente': return 'bg-yellow-100 text-yellow-800 border-yellow-200';
            case 'Aceito': return 'bg-blue-100 text-blue-800 border-blue-200';
            case 'Saiu para Entrega': return 'bg-purple-100 text-purple-800 border-purple-200';
            case 'Entregue': return 'bg-green-100 text-green-800 border-green-200';
            case 'Cancelado': return 'bg-red-100 text-red-800 border-red-200';
            default: return 'bg-slate-100 text-slate-800 border-slate-200';
        }
    };

    const StatusIcon = ({ status }: { status: string }) => {
        switch (status) {
            case 'Pendente': return <Clock className="w-4 h-4"/>;
            case 'Aceito': return <CheckCircle className="w-4 h-4"/>;
            case 'Saiu para Entrega': return <Truck className="w-4 h-4"/>;
            case 'Entregue': return <Package className="w-4 h-4"/>;
            case 'Cancelado': return <XCircle className="w-4 h-4"/>;
            default: return <Clock className="w-4 h-4"/>;
        }
    }

    return (
        <div className="space-y-6">
            <div className="flex justify-between items-center">
                <h1 className="text-2xl font-bold text-slate-800">Pedidos Recentes</h1>
                <button onClick={fetchOrders} className="text-blue-600 font-medium hover:underline text-sm">
                    Atualizar Lista
                </button>
            </div>

            {loading ? (
                 <div className="text-center py-20 text-slate-500">Carregando pedidos...</div>
            ) : orders.length === 0 ? (
                 <div className="text-center py-20 text-slate-500 bg-white rounded-xl border border-slate-200">
                    Nenhum pedido encontrado.
                 </div>
            ) : (
                <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
                    {orders.map(order => (
                        <div key={order.firestoreId} className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden hover:shadow-md transition-shadow">
                            
                            {/* Header */}
                            <div className="bg-slate-50 p-4 border-b border-slate-100 flex justify-between items-start">
                                <div>
                                    <div className="flex items-center gap-2 mb-1">
                                        <span className={`px-2 py-1 rounded-full text-xs font-bold flex items-center gap-1 border ${getStatusColor(order.status)}`}>
                                            <StatusIcon status={order.status} /> {order.status}
                                        </span>
                                        <span className="text-xs text-slate-400">
                                            {new Date(order.date).toLocaleString()}
                                        </span>
                                    </div>
                                    <h3 className="font-bold text-slate-800">{order.customerName}</h3>
                                </div>
                                <div className="text-right">
                                    <span className="block font-bold text-lg text-blue-600">R$ {order.totalAmount.toFixed(2)}</span>
                                    <span className="text-xs text-slate-500">{order.paymentMethod}</span>
                                </div>
                            </div>

                            {/* Details */}
                            <div className="p-4 space-y-4">
                                
                                {/* Address */}
                                <div className="flex items-start gap-2 text-sm text-slate-600">
                                    <MapPin className="w-4 h-4 mt-0.5 text-slate-400"/>
                                    <div>
                                        <p className="line-clamp-2">{order.deliveryAddress}</p>
                                        <p className="text-xs text-slate-400">{order.addressNeighborhood}</p>
                                    </div>
                                </div>

                                {/* Phone */}
                                <div className="flex items-center gap-2 text-sm text-slate-600">
                                    <Phone className="w-4 h-4 text-slate-400"/>
                                    <span>{order.customerPhone}</span>
                                </div>

                                {/* Items (Collapsed) */}
                                <div className="bg-slate-50 rounded-lg p-3 text-sm space-y-1">
                                    {order.items?.slice(0, 3).map((item, idx) => (
                                        <div key={idx} className="flex justify-between">
                                            <span className="text-slate-700 truncate w-2/3">{item.quantity}x {item.name}</span>
                                            <span className="text-slate-500 font-medium">{item.price.toFixed(2)}</span>
                                        </div>
                                    ))}
                                    {order.items && order.items.length > 3 && (
                                        <p className="text-xs text-center text-slate-400 pt-1">+ {order.items.length - 3} itens</p>
                                    )}
                                </div>

                                {/* Quick Actions */}
                                <div className="pt-2 flex gap-2 overflow-x-auto no-scrollbar">
                                    {order.status === 'Pendente' && (
                                        <button 
                                            onClick={() => handleStatusChange(order.firestoreId, 'Aceito')}
                                            className="flex-1 bg-blue-600 text-white text-sm font-bold py-2 rounded-lg hover:bg-blue-700"
                                        >
                                            Aceitar
                                        </button>
                                    )}
                                    {order.status === 'Aceito' && (
                                        <button 
                                            onClick={() => handleStatusChange(order.firestoreId, 'Saiu para Entrega')}
                                            className="flex-1 bg-purple-600 text-white text-sm font-bold py-2 rounded-lg hover:bg-purple-700"
                                        >
                                            Despachar
                                        </button>
                                    )}
                                    {order.status === 'Saiu para Entrega' && (
                                        <button 
                                            onClick={() => handleStatusChange(order.firestoreId, 'Entregue')}
                                            className="flex-1 bg-green-600 text-white text-sm font-bold py-2 rounded-lg hover:bg-green-700"
                                        >
                                            Concluir
                                        </button>
                                    )}
                                    {order.status !== 'Cancelado' && order.status !== 'Entregue' && (
                                        <button 
                                            onClick={() => handleStatusChange(order.firestoreId, 'Cancelado')}
                                            className="px-3 py-2 bg-red-50 text-red-600 hover:bg-red-100 rounded-lg text-sm font-medium"
                                        >
                                            Cancelar
                                        </button>
                                    )}
                                </div>
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
}
