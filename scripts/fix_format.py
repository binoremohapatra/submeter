import os

files = [
    r'd:\submeter\frontend\src\app\dashboard\analytics\page.tsx',
    r'd:\submeter\frontend\src\app\dashboard\customers\[id]\page.tsx',
    r'd:\submeter\frontend\src\app\dashboard\invoices\page.tsx',
    r'd:\submeter\frontend\src\app\dashboard\invoices\[id]\page.tsx',
    r'd:\submeter\frontend\src\app\dashboard\page.tsx',
    r'd:\submeter\frontend\src\app\dashboard\plans\page.tsx',
    r'd:\submeter\frontend\src\app\dashboard\plans\[id]\page.tsx',
    r'd:\submeter\frontend\src\app\dashboard\subscriptions\[id]\page.tsx'
]

correct_format = """function formatINR(cents: number) {
  const code = typeof window !== "undefined" ? localStorage.getItem("currency_code") || "INR" : "INR";
  const val = (cents / 100);
  switch (code) {
    case "USD": return "$" + val.toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    case "EUR": return "€" + val.toLocaleString("en-DE", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    case "GBP": return "£" + val.toLocaleString("en-GB", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    default: return "₹" + val.toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }
}"""

for fpath in files:
    with open(fpath, 'r', encoding='utf-8', errors='ignore') as f:
        content = f.read()
    
    idx1 = content.find('function formatINR(cents: number) {')
    if idx1 == -1: continue
    
    idx_func = content.find('\nfunction ', idx1 + 10)
    idx_export = content.find('\nexport ', idx1 + 10)
    
    next_idx = -1
    if idx_func != -1 and idx_export != -1:
        next_idx = min(idx_func, idx_export)
    elif idx_func != -1:
        next_idx = idx_func
    else:
        next_idx = idx_export
        
    if next_idx != -1:
        new_content = content[:idx1] + correct_format + content[next_idx:]
        with open(fpath, 'w', encoding='utf-8') as f:
            f.write(new_content)
        print('Fixed', fpath)
