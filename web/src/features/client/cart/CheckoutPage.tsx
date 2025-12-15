import { useState } from 'react';
import { useAppStore } from '../../../store';
import { useNavigate } from 'react-router-dom';
import { addDoc, collection } from 'firebase/firestore';
import { db } from '../../../lib/firebase';
import { ArrowLeft, MapPin, CreditCard, CheckCircle } from 'lucide-react';
// import type { CartItemSummary } from '../../../types';

export default function CheckoutPage() {
    const { cart, clearCart, user } = useAppStore();
    const navigate = useNavigate();
    const [loading, setLoading] = useState(false);

    const [form, setForm] = useState({
        name: user?.name || '',
        phone: user?.whatsapp || '',
        address: user?.address || '',
        neighborhood: user?.neighborhood || '',
        paymentMethod: 'Pix'
    });

    const subtotal = cart.reduce((acc, item) => acc + (item.price * item.quantity), 0);
    const deliveryFee = 5.00;
    const total = subtotal + deliveryFee;

    const handlePlaceOrder = async (e: React.FormEvent) => {
        e.preventDefault();
        setLoading(true);

        try {
            // Construct Order object matching Android data model
            const orderData = {
                userId: user?.id || 'GUEST',
                storeId: 'GLOBAL', // or selectedStore.id
                storeName: 'PharmaFlow Matriz',
                
                totalAmount: total,
                subtotalAmount: subtotal,
                discountAmount: 0,
                deliveryFee: deliveryFee,
                pointsUsed: 0,
                
                paymentMethod: form.paymentMethod,
                isDelivery: true,
                deliveryAddress: form.address,
                addressNeighborhood: form.neighborhood,
                addressComplement: '',
                
                date: Date.now(),
                status: 'Pendente',
                
                customerName: form.name,
                customerPhone: form.phone,
                
                // Android uses a JSON string for items in Room, but likely an array in Firestore.
                // We will save as array 'items' AND 'itemsJson' to be safe for both.
                items: cart.map(c => ({
                    name: c.name,
                    quantity: c.quantity,
                    price: c.price,
                    imageUrl: c.imageUrl
                })),
                itemsJson: JSON.stringify(cart.map(c => ({
                    name: c.name,
                    quantity: c.quantity,
                    price: c.price,
                    imageUrl: c.imageUrl
                }))),
                
                cancellationReason: '',
                driverId: '',
                driverName: ''
            };

            await addDoc(collection(db, 'orders'), orderData);
            
            clearCart();
            alert('Pedido realizado com sucesso!');
            navigate('/'); // In a real app, go to Order Success page
            
        } catch (error) {
            console.error("Error placing order", error);
            alert("Erro ao realizar pedido.");
        } finally {
            setLoading(false);
        }
    };

    if (cart.length === 0) {
        navigate('/cart');
        return null;
    }

    return (
        <div className="max-w-2xl mx-auto pb-20">
            <button onClick={() => navigate(-1)} className="flex items-center gap-2 text-slate-500 mb-6 hover:text-slate-800">
                <ArrowLeft className="w-4 h-4"/> Voltar
            </button>
            
            <h1 className="text-2xl font-bold text-slate-800 mb-6">Finalizar Pagamento</h1>

            <form onSubmit={handlePlaceOrder} className="space-y-6">
                
                {/* Personal Info */}
                <div className="bg-white p-6 rounded-xl border border-slate-200 shadow-sm sections">
                    <h3 className="font-bold text-slate-800 mb-4 flex items-center gap-2">
                        <MapPin className="w-5 h-5 text-blue-600"/> Dados de Entrega
                    </h3>
                    <div className="space-y-4">
                        <div>
                            <label className="block text-sm font-medium text-slate-700 mb-1">Seu Nome</label>
                            <input 
                                required
                                value={form.name}
                                onChange={e => setForm({...form, name: e.target.value})}
                                className="w-full p-3 rounded-lg border border-slate-200 focus:ring-2 focus:ring-blue-500 outline-none"
                                placeholder="João da Silva"
                            />
                        </div>
                        <div>
                            <label className="block text-sm font-medium text-slate-700 mb-1">WhatsApp / Telefone</label>
                            <input 
                                required
                                value={form.phone}
                                onChange={e => setForm({...form, phone: e.target.value})}
                                className="w-full p-3 rounded-lg border border-slate-200 focus:ring-2 focus:ring-blue-500 outline-none"
                                placeholder="(13) 99999-9999"
                            />
                        </div>
                        <div className="grid grid-cols-2 gap-4">
                            <div className="col-span-2">
                                <label className="block text-sm font-medium text-slate-700 mb-1">Endereço Completo</label>
                                <input 
                                    required
                                    value={form.address}
                                    onChange={e => setForm({...form, address: e.target.value})}
                                    className="w-full p-3 rounded-lg border border-slate-200 focus:ring-2 focus:ring-blue-500 outline-none"
                                    placeholder="Rua das Flores, 123"
                                />
                            </div>
                            <div className="col-span-2 md:col-span-1">
                                <label className="block text-sm font-medium text-slate-700 mb-1">Bairro</label>
                                <input 
                                    required
                                    value={form.neighborhood}
                                    onChange={e => setForm({...form, neighborhood: e.target.value})}
                                    className="w-full p-3 rounded-lg border border-slate-200 focus:ring-2 focus:ring-blue-500 outline-none"
                                    placeholder="Enseada"
                                />
                            </div>
                        </div>
                    </div>
                </div>

                {/* Payment */}
                <div className="bg-white p-6 rounded-xl border border-slate-200 shadow-sm sections">
                    <h3 className="font-bold text-slate-800 mb-4 flex items-center gap-2">
                         <CreditCard className="w-5 h-5 text-blue-600"/> Pagamento
                    </h3>
                    <div className="grid grid-cols-2 gap-4">
                        {['Pix', 'Dinheiro', 'Cartão na Entrega'].map(method => (
                            <button
                                key={method}
                                type="button"
                                onClick={() => setForm({...form, paymentMethod: method})}
                                className={`p-4 rounded-xl border text-sm font-medium transition-all ${
                                    form.paymentMethod === method 
                                        ? 'bg-blue-50 border-blue-500 text-blue-700 ring-1 ring-blue-500' 
                                        : 'bg-white border-slate-200 text-slate-600 hover:border-blue-300'
                                }`}
                            >
                                {method}
                            </button>
                        ))}
                    </div>
                </div>

                {/* Total & Submit */}
                <div className="bg-slate-50 p-6 rounded-xl border border-slate-200">
                    <div className="flex justify-between items-center mb-6">
                        <span className="text-slate-600">Total a pagar:</span>
                        <span className="text-2xl font-bold text-slate-900">R$ {total.toFixed(2)}</span>
                    </div>
                    
                    <button 
                        type="submit"
                        disabled={loading}
                        className="w-full bg-green-600 text-white py-4 rounded-xl font-bold text-lg hover:bg-green-700 transition-all shadow-lg shadow-green-200 flex items-center justify-center gap-2 disabled:opacity-50"
                    >
                        {loading ? 'Processando...' : 'Confirmar Pedido'} 
                        {!loading && <CheckCircle className="w-5 h-5"/>}
                    </button>
                    <p className="text-center text-xs text-slate-500 mt-4">
                        Ao confirmar, você concorda com nossos termos de serviço.
                    </p>
                </div>
            </form>
        </div>
    );
}
