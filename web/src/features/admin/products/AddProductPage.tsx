import { useState } from 'react';
import { collection, addDoc } from 'firebase/firestore';
import { db } from '../../../lib/firebase';
import { useNavigate } from 'react-router-dom';
import { Save, ArrowLeft } from 'lucide-react';

export default function AddProductPage() {
    const navigate = useNavigate();
    const [submitting, setSubmitting] = useState(false);
    
    // Form State
    const [formData, setFormData] = useState({
        name: '',
        ean: '',
        price: '',
        promoPrice: '',
        stockQuantity: '',
        category: '',
        description: '',
        imageUrl: '' // In real app, we would have Image Upload logic here
    });

    const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
        setFormData({ ...formData, [e.target.name]: e.target.value });
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setSubmitting(true);
        try {
            await addDoc(collection(db, 'products'), {
                ...formData,
                price: Number(formData.price) || 0,
                promoPrice: Number(formData.promoPrice) || 0,
                stockQuantity: Number(formData.stockQuantity) || 0,
                isActive: true, // Default
                isFeatured: false,
                expirationDate: 0,
                internalCode: '',
                storeId: 'GLOBAL', // Default for now
                searchIndex: `${formData.name} ${formData.ean} ${formData.category}`.toLowerCase()
            });
            alert('Produto salvo com sucesso!');
            navigate('/admin/products');
        } catch (error) {
            console.error("Error adding product:", error);
            alert("Erro ao salvar produto.");
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <div className="max-w-3xl mx-auto space-y-6 pb-20">
            <div className="flex items-center gap-4">
                <button onClick={() => navigate(-1)} className="p-2 hover:bg-slate-100 rounded-lg text-slate-500">
                    <ArrowLeft className="w-5 h-5"/>
                </button>
                <h1 className="text-2xl font-bold text-slate-800">Novo Produto</h1>
            </div>

            <form onSubmit={handleSubmit} className="bg-white rounded-xl shadow-sm border border-slate-200 p-6 space-y-6">
                
                {/* Basic Info */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    <div className="space-y-2 col-span-2">
                        <label className="text-sm font-medium text-slate-700">Nome do Produto</label>
                        <input 
                            required
                            name="name"
                            value={formData.name}
                            onChange={handleChange}
                            className="w-full p-3 rounded-lg border border-slate-200 focus:ring-2 focus:ring-blue-500 outline-none" 
                            placeholder="Ex: Tylenol 750mg c/ 20 comprimidos"
                        />
                    </div>

                    <div className="space-y-2">
                        <label className="text-sm font-medium text-slate-700">Código EAN</label>
                        <input 
                            name="ean"
                            value={formData.ean}
                            onChange={handleChange}
                            className="w-full p-3 rounded-lg border border-slate-200 focus:ring-2 focus:ring-blue-500 outline-none" 
                            placeholder="789..."
                        />
                    </div>
                    
                    <div className="space-y-2">
                        <label className="text-sm font-medium text-slate-700">Categoria</label>
                        <select 
                            name="category"
                            value={formData.category}
                            onChange={handleChange}
                            className="w-full p-3 rounded-lg border border-slate-200 focus:ring-2 focus:ring-blue-500 outline-none bg-white"
                        >
                            <option value="">Selecione...</option>
                            <option value="Medicamentos">Medicamentos</option>
                            <option value="Higiene">Higiene</option>
                            <option value="Beleza">Beleza</option>
                            <option value="Infantil">Infantil</option>
                            <option value="Fitness">Fitness</option>
                        </select>
                    </div>
                </div>

                {/* Pricing & Stock */}
                <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                    <div className="space-y-2">
                        <label className="text-sm font-medium text-slate-700">Preço (R$)</label>
                        <input 
                            type="number"
                            step="0.01"
                            required
                            name="price"
                            value={formData.price}
                            onChange={handleChange}
                            className="w-full p-3 rounded-lg border border-slate-200 focus:ring-2 focus:ring-blue-500 outline-none" 
                            placeholder="0.00"
                        />
                    </div>
                    
                    <div className="space-y-2">
                        <label className="text-sm font-medium text-slate-700">Preço Promo (R$)</label>
                        <input 
                            type="number"
                            step="0.01"
                            name="promoPrice"
                            value={formData.promoPrice}
                            onChange={handleChange}
                            className="w-full p-3 rounded-lg border border-slate-200 focus:ring-2 focus:ring-blue-500 outline-none" 
                            placeholder="0.00"
                        />
                    </div>

                    <div className="space-y-2">
                        <label className="text-sm font-medium text-slate-700">Estoque (Qtd)</label>
                        <input 
                            type="number"
                            required
                            name="stockQuantity"
                            value={formData.stockQuantity}
                            onChange={handleChange}
                            className="w-full p-3 rounded-lg border border-slate-200 focus:ring-2 focus:ring-blue-500 outline-none" 
                            placeholder="0"
                        />
                    </div>
                </div>

                {/* Image URL (Temporary) */}
                <div className="space-y-2">
                    <label className="text-sm font-medium text-slate-700">URL da Imagem</label>
                    <input 
                        name="imageUrl"
                        value={formData.imageUrl}
                        onChange={handleChange}
                        className="w-full p-3 rounded-lg border border-slate-200 focus:ring-2 focus:ring-blue-500 outline-none" 
                        placeholder="https://..."
                    />
                    <p className="text-xs text-slate-400">Em breve: Upload de arquivo direto para o Storage</p>
                </div>

                <div className="space-y-2">
                    <label className="text-sm font-medium text-slate-700">Descrição</label>
                    <textarea 
                        name="description"
                        value={formData.description}
                        onChange={handleChange}
                        rows={4}
                        className="w-full p-3 rounded-lg border border-slate-200 focus:ring-2 focus:ring-blue-500 outline-none resize-none" 
                        placeholder="Detalhes do produto..."
                    />
                </div>

                <div className="flex justify-end pt-4">
                    <button 
                        type="submit" 
                        disabled={submitting}
                        className="bg-blue-600 text-white px-8 py-3 rounded-xl font-bold hover:bg-blue-700 transition-colors flex items-center gap-2 disabled:opacity-50"
                    >
                        <Save className="w-5 h-5" />
                        {submitting ? 'Salvando...' : 'Salvar Produto'}
                    </button>
                </div>

            </form>
        </div>
    );
}
