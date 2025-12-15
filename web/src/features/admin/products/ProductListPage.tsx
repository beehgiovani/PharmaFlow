import { useEffect, useState } from 'react';
import { collection, getDocs, deleteDoc, doc } from 'firebase/firestore';
import { db } from '../../../lib/firebase';
import type { Product } from '../../../types';
import { Plus, Edit, Trash2, Search } from 'lucide-react';
import { Link } from 'react-router-dom';

export default function ProductListPage() {
    const [products, setProducts] = useState<Product[]>([]);
    const [loading, setLoading] = useState(true);
    const [searchTerm, setSearchTerm] = useState('');

    useEffect(() => {
        fetchProducts();
    }, []);

    async function fetchProducts() {
        try {
            // In a real scenario with many products, we should implement pagination
            const querySnapshot = await getDocs(collection(db, "products"));
            const list = querySnapshot.docs.map(doc => ({
                id: doc.id,
                ...doc.data()
            })) as Product[];
            setProducts(list);
        } catch (error) {
            console.error("Error fetching products:", error);
        } finally {
            setLoading(false);
        }
    }

    async function handleDelete(id: string) {
        if (!confirm('Tem certeza que deseja excluir este produto?')) return;
        try {
            await deleteDoc(doc(db, 'products', id));
            setProducts(products.filter(p => p.id !== id));
        } catch (error) {
            console.error("Error deleting product:", error);
            alert('Erro ao excluir produto');
        }
    }

    const filtered = products.filter(p => 
        p.name.toLowerCase().includes(searchTerm.toLowerCase()) || 
        p.ean.includes(searchTerm)
    );

    return (
        <div className="space-y-6">
            <div className="flex justify-between items-center">
                <h1 className="text-2xl font-bold text-slate-800">Produtos</h1>
                <Link 
                    to="/admin/products/new" 
                    className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg flex items-center gap-2 transition-colors"
                >
                    <Plus className="w-5 h-5"/> Novo Produto
                </Link>
            </div>

            {/* Search Bar */}
            <div className="relative">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 w-5 h-5" />
                <input 
                    type="text" 
                    placeholder="Buscar por nome, EAN ou código..." 
                    className="w-full pl-10 pr-4 py-3 rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-blue-500 transition-all"
                    value={searchTerm}
                    onChange={e => setSearchTerm(e.target.value)}
                />
            </div>

            {/* Product Table */}
            <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
                {loading ? (
                    <div className="p-8 text-center text-slate-500">Carregando estoque...</div>
                ) : filtered.length === 0 ? (
                    <div className="p-8 text-center text-slate-500">Nenhum produto encontrado.</div>
                ) : (
                    <table className="w-full text-left border-collapse">
                        <thead className="bg-slate-50 text-slate-500 text-sm font-semibold uppercase tracking-wider">
                            <tr>
                                <th className="p-4">Produto</th>
                                <th className="p-4">Preço</th>
                                <th className="p-4">Estoque</th>
                                <th className="p-4">Categoria</th>
                                <th className="p-4 text-right">Ações</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-100">
                            {filtered.map(product => (
                                <tr key={product.id} className="hover:bg-slate-50 transition-colors">
                                    <td className="p-4">
                                        <div className="flex items-center gap-3">
                                            <div className="w-12 h-12 rounded-lg bg-slate-100 overflow-hidden flex-shrink-0 border border-slate-200">
                                                {product.imageUrl ? (
                                                    <img src={product.imageUrl} alt={product.name} className="w-full h-full object-cover" />
                                                ) : (
                                                    <div className="w-full h-full flex items-center justify-center text-slate-300 text-xs text-center p-1">Sem img</div>
                                                )}
                                            </div>
                                            <div>
                                                <p className="font-medium text-slate-900 line-clamp-1">{product.name}</p>
                                                <p className="text-xs text-slate-400">{product.ean}</p>
                                            </div>
                                        </div>
                                    </td>
                                    <td className="p-4">
                                        <div className="flex flex-col">
                                            {product.promoPrice > 0 ? (
                                                <>
                                                    <span className="text-green-600 font-bold">R$ {product.promoPrice.toFixed(2)}</span>
                                                    <span className="text-slate-400 text-xs line-through">R$ {product.price.toFixed(2)}</span>
                                                </>
                                            ) : (
                                                <span className="text-slate-700 font-medium">R$ {product.price.toFixed(2)}</span>
                                            )}
                                        </div>
                                    </td>
                                    <td className="p-4">
                                        <span className={`px-2 py-1 rounded-full text-xs font-bold ${
                                            product.stockQuantity > 10 ? 'bg-green-100 text-green-700' : 
                                            product.stockQuantity > 0 ? 'bg-yellow-100 text-yellow-700' :
                                            'bg-red-100 text-red-700'
                                        }`}>
                                            {product.stockQuantity} un
                                        </span>
                                    </td>
                                    <td className="p-4 text-slate-600 text-sm">
                                        {product.category || '-'}
                                    </td>
                                    <td className="p-4 text-right">
                                        <div className="flex items-center justify-end gap-2">
                                            <button className="p-2 text-slate-400 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition-colors">
                                                <Edit className="w-4 h-4" />
                                            </button>
                                            <button 
                                                onClick={() => handleDelete(product.id)}
                                                className="p-2 text-slate-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                                            >
                                                <Trash2 className="w-4 h-4" />
                                            </button>
                                        </div>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                )}
            </div>
        </div>
    );
}
