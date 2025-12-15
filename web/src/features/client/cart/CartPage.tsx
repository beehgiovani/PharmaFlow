import { useAppStore } from '../../../store';
import { Plus, Minus, Trash2, ArrowRight } from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';

export default function CartPage() {
    const { cart, removeFromCart, updateQuantity, clearCart } = useAppStore();
    const navigate = useNavigate();

    const subtotal = cart.reduce((acc, item) => acc + (item.price * item.quantity), 0);
    const deliveryFee = 5.00; // Mock fee
    const total = subtotal + deliveryFee;

    if (cart.length === 0) {
        return (
            <div className="flex flex-col items-center justify-center py-20 text-center space-y-4">
                <div className="w-24 h-24 bg-slate-100 rounded-full flex items-center justify-center mb-4">
                   <div className="w-12 h-12 text-slate-300">🛒</div>
                </div>
                <h2 className="text-xl font-bold text-slate-800">Seu carrinho está vazio</h2>
                <p className="text-slate-500 max-w-xs">Adicione produtos do catálogo para realizar seu pedido.</p>
                <Link to="/" className="text-blue-600 font-bold hover:underline">
                    Voltar para a Loja
                </Link>
            </div>
        );
    }

    return (
        <div className="max-w-4xl mx-auto pb-20">
            <h1 className="text-2xl font-bold text-slate-800 mb-6">Carrinho de Compras</h1>
            
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                {/* Items List */}
                <div className="lg:col-span-2 space-y-4">
                    {cart.map(item => (
                        <div key={item.productId} className="bg-white p-4 rounded-xl border border-slate-100 shadow-sm flex items-center gap-4">
                            <div className="w-20 h-20 bg-slate-50 rounded-lg flex-shrink-0 flex items-center justify-center overflow-hidden">
                                {item.imageUrl ? (
                                    <img src={item.imageUrl} alt={item.name} className="w-full h-full object-cover" />
                                ) : (
                                    <span className="text-slate-300">Img</span>
                                )}
                            </div>
                            
                            <div className="flex-1">
                                <h3 className="font-medium text-slate-800 line-clamp-1">{item.name}</h3>
                                <p className="text-blue-600 font-bold">R$ {item.price.toFixed(2)}</p>
                            </div>

                            <div className="flex flex-col items-end gap-2">
                                <button 
                                    onClick={() => removeFromCart(item.productId)}
                                    className="text-slate-400 hover:text-red-500 p-1"
                                >
                                    <Trash2 className="w-4 h-4" />
                                </button>
                                
                                <div className="flex items-center gap-3 bg-slate-50 rounded-lg p-1">
                                    <button 
                                        onClick={() => updateQuantity(item.productId, item.quantity - 1)}
                                        className="w-8 h-8 flex items-center justify-center bg-white rounded shadow-sm text-slate-600 hover:bg-slate-100"
                                    >
                                        <Minus className="w-3 h-3" />
                                    </button>
                                    <span className="text-sm font-bold w-4 text-center">{item.quantity}</span>
                                    <button 
                                        onClick={() => updateQuantity(item.productId, item.quantity + 1)}
                                        className="w-8 h-8 flex items-center justify-center bg-white rounded shadow-sm text-blue-600 hover:bg-slate-100"
                                    >
                                        <Plus className="w-3 h-3" />
                                    </button>
                                </div>
                            </div>
                        </div>
                    ))}
                    
                    <button 
                         onClick={() => clearCart()}
                         className="text-red-500 text-sm font-medium hover:underline flex items-center gap-1"
                    >
                        <Trash2 className="w-4 h-4" /> Limpar Carrinho
                    </button>
                </div>

                {/* Summary */}
                <div className="lg:col-span-1">
                    <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-100 sticky top-24">
                        <h3 className="font-bold text-slate-800 mb-4">Resumo do Pedido</h3>
                        
                        <div className="space-y-3 mb-6">
                            <div className="flex justify-between text-slate-600">
                                <span>Subtotal</span>
                                <span>R$ {subtotal.toFixed(2)}</span>
                            </div>
                            <div className="flex justify-between text-slate-600">
                                <span>Taxa de Entrega</span>
                                <span>R$ {deliveryFee.toFixed(2)}</span>
                            </div>
                            <div className="h-px bg-slate-100 my-2"></div>
                            <div className="flex justify-between text-lg font-bold text-slate-900">
                                <span>Total</span>
                                <span>R$ {total.toFixed(2)}</span>
                            </div>
                        </div>

                        <button 
                            onClick={() => navigate('/checkout')}
                            className="w-full bg-green-600 text-white py-4 rounded-xl font-bold text-lg hover:bg-green-700 transition-all shadow-green-200 shadow-lg flex items-center justify-center gap-2 active:scale-95"
                        >
                            Finalizar Compra <ArrowRight className="w-5 h-5" />
                        </button>
                        
                        <Link to="/" className="block text-center mt-4 text-slate-500 text-sm hover:underline">
                            Continuar Comprando
                        </Link>
                    </div>
                </div>
            </div>
        </div>
    );
}
