import { useEffect, useState } from 'react';
import { collection, query, getDocs, limit } from 'firebase/firestore';
import { db } from '../../lib/firebase';
import type { Product } from '../../types';
import { useAppStore } from '../../store';
import { Plus, Search } from 'lucide-react';

export default function HomePage() {
  const [featuredProducts, setFeaturedProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const addToCart = useAppStore(state => state.addToCart);

  useEffect(() => {
    fetchFeaturedProducts();
  }, []);

  async function fetchFeaturedProducts() {
    try {
        // Fetch products where isFeatured is true, or just fetch first 10 for demo if no featured items
        // Note: Composite index might be needed for specific queries
        const q = query(collection(db, "products"), limit(20));
        const snapshot = await getDocs(q);
        const list = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() })) as Product[];
        setFeaturedProducts(list);
    } catch (error) {
        console.error("Error fetching home products", error);
    } finally {
        setLoading(false);
    }
  }

  return (
    <div className="space-y-8 pb-20">
       {/* Hero / Promotions Banner */}
       <section className="bg-gradient-to-r from-blue-600 to-indigo-700 rounded-2xl p-6 md:p-10 text-white shadow-lg relative overflow-hidden">
          <div className="relative z-10 max-w-lg">
             <h2 className="text-3xl md:text-4xl font-bold mb-4">Saúde e Bem-estar na sua porta.</h2>
             <p className="text-blue-100 text-lg mb-6">Encontrou mais barato? A gente cobre a oferta! Entrega grátis para primeira compra.</p>
             <button className="bg-white text-blue-700 px-6 py-3 rounded-full font-bold shadow-md hover:bg-blue-50 transition-colors">
                Ver Ofertas
             </button>
          </div>
          {/* Decorative circles */}
          <div className="absolute right-0 top-0 w-64 h-64 bg-white/10 rounded-full -translate-y-1/2 translate-x-1/3 blur-3xl"></div>
          <div className="absolute bottom-0 left-1/4 w-32 h-32 bg-indigo-500/30 rounded-full translate-y-1/2 blur-2xl"></div>
       </section>

       {/* Search Bar Mobile */}
       <div className="md:hidden relative">
          <input 
             type="text" 
             placeholder="O que você procura hoje?" 
             className="w-full pl-10 pr-4 py-3 rounded-xl border border-neutral-200 bg-white shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-neutral-400 w-5 h-5" />
       </div>
       
       {/* Categories Quick Links (Mock) */}
       <div className="flex gap-4 overflow-x-auto pb-2 no-scrollbar">
          {['Medicamentos', 'Higiene', 'Beleza', 'Mamãe e Bebê', 'Fitness'].map(cat => (
             <button key={cat} className="flex-shrink-0 px-4 py-2 bg-white border border-neutral-200 rounded-full text-sm font-medium text-neutral-600 hover:border-blue-500 hover:text-blue-600 transition-colors">
                {cat}
             </button>
          ))}
       </div>

       <div>
           <h3 className="text-xl font-bold text-neutral-800 mb-4">Destaques</h3>
           
           {loading ? (
             <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-5 gap-4">
                 {[1,2,3,4,5].map(i => (
                    <div key={i} className="bg-white p-4 rounded-xl border border-neutral-100 h-64 animate-pulse">
                       <div className="h-32 bg-neutral-200 rounded-lg mb-4"></div>
                       <div className="h-4 w-3/4 bg-neutral-200 rounded mb-2"></div>
                       <div className="h-4 w-1/2 bg-neutral-200 rounded"></div>
                    </div>
                 ))}
             </div>
           ) : (
             <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-5 gap-4">
                {featuredProducts.map(product => (
                  <div key={product.id} className="bg-white p-4 rounded-xl border border-neutral-100 shadow-sm hover:shadow-md transition-all group flex flex-col h-full">
                     <div className="h-32 mb-4 relative flex items-center justify-center bg-neutral-50 rounded-lg overflow-hidden">
                        {product.imageUrl ? (
                            <img src={product.imageUrl} alt={product.name} className="max-h-full max-w-full object-contain group-hover:scale-110 transition-transform duration-300" />
                        ) : (
                            <Search className="w-8 h-8 text-neutral-300" />
                        )}
                        {product.promoPrice > 0 && (
                            <span className="absolute top-2 left-2 bg-green-500 text-white text-[10px] font-bold px-2 py-1 rounded-full">
                                PROMO
                            </span>
                        )}
                     </div>
                     
                     <div className="flex-1">
                        <h4 className="font-medium text-neutral-800 text-sm line-clamp-2 mb-1" title={product.name}>{product.name}</h4>
                        <p className="text-xs text-neutral-400 mb-2 truncate">{product.category}</p>
                     </div>
                     
                     <div className="mt-2">
                        <div className="flex items-baseline gap-2 mb-3">
                           {product.promoPrice > 0 ? (
                             <>
                                <span className="text-blue-600 font-bold">R$ {product.promoPrice.toFixed(2)}</span>
                                <span className="text-neutral-400 text-xs line-through">R$ {product.price.toFixed(2)}</span>
                             </>
                           ) : (
                                <span className="text-blue-600 font-bold">R$ {product.price.toFixed(2)}</span>
                           )}
                        </div>
                        
                        <button 
                            onClick={() => addToCart(product)}
                            className="w-full bg-blue-50 text-blue-600 hover:bg-blue-600 hover:text-white font-medium py-2 rounded-lg text-sm transition-all flex items-center justify-center gap-2 active:scale-95"
                        >
                            <Plus className="w-4 h-4" /> Adicionar
                        </button>
                     </div>
                  </div>
                ))}
             </div>
           )}
       </div>
    </div>
  );
}
