
export default function DashboardPage() {
  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-slate-800">Dashboard</h1>
      
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {[
           { label: 'Total Revenue', value: 'R$ 12.450', color: 'bg-blue-500' },
           { label: 'Active Orders', value: '24', color: 'bg-indigo-500' },
           { label: 'New Customers', value: '18', color: 'bg-emerald-500' },
        ].map((stat, i) => (
           <div key={i} className="bg-white p-6 rounded-2xl shadow-sm border border-slate-100 flex items-center justify-between">
              <div>
                <p className="text-slate-500 text-sm font-medium">{stat.label}</p>
                <p className="text-2xl font-bold text-slate-800 mt-1">{stat.value}</p>
              </div>
              <div className={`w-10 h-10 rounded-full ${stat.color} opacity-10 flex items-center justify-center`}>
                <div className={`w-5 h-5 ${stat.color} opacity-100 rounded-full`}></div>
              </div>
           </div>
        ))}
      </div>
    </div>
  );
}
