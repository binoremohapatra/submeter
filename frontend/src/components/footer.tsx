import Link from "next/link";
import { Zap } from "lucide-react";

export function Footer() {
  return (
    <footer className="border-t border-white/10 bg-gray-950 py-12 md:py-16">
      <div className="container mx-auto px-4 md:px-8">
        <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-5 gap-8">
          <div className="col-span-2 lg:col-span-2">
            <Link href="/" className="flex items-center gap-2 mb-4">
              <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-emerald-500/10 border border-emerald-500/20">
                <Zap className="h-4 w-4 text-emerald-500" />
              </div>
              <span className="text-xl font-medium tracking-tight text-gray-100">
                SubMeter
              </span>
            </Link>
            <p className="text-gray-400 text-sm mb-6 max-w-xs">
              Usage-metered subscription billing with real-time MRR dashboards. Designed for modern SaaS.
            </p>
          </div>

          <div>
            <h3 className="text-gray-100 font-medium mb-4 text-sm uppercase tracking-wider">Product</h3>
            <ul className="flex flex-col gap-3">
              <li><Link href="/#features" className="text-gray-400 hover:text-gray-200 text-sm transition-colors">Features</Link></li>
              <li><Link href="/pricing" className="text-gray-400 hover:text-gray-200 text-sm transition-colors">Pricing</Link></li>
              <li><Link href="/changelog" className="text-gray-400 hover:text-gray-200 text-sm transition-colors">Changelog</Link></li>
            </ul>
          </div>

          <div>
            <h3 className="text-gray-100 font-medium mb-4 text-sm uppercase tracking-wider">Developers</h3>
            <ul className="flex flex-col gap-3">
              <li><Link href="/docs" className="text-gray-400 hover:text-gray-200 text-sm transition-colors">Documentation</Link></li>
              <li><Link href="/api" className="text-gray-400 hover:text-gray-200 text-sm transition-colors">API Reference</Link></li>
              <li><Link href="/status" className="text-gray-400 hover:text-gray-200 text-sm transition-colors">Status</Link></li>
            </ul>
          </div>

          <div>
            <h3 className="text-gray-100 font-medium mb-4 text-sm uppercase tracking-wider">Company</h3>
            <ul className="flex flex-col gap-3">
              <li><Link href="/about" className="text-gray-400 hover:text-gray-200 text-sm transition-colors">About</Link></li>
              <li><Link href="/blog" className="text-gray-400 hover:text-gray-200 text-sm transition-colors">Blog</Link></li>
              <li><Link href="/contact" className="text-gray-400 hover:text-gray-200 text-sm transition-colors">Contact</Link></li>
            </ul>
          </div>
        </div>
        
        <div className="border-t border-white/10 mt-12 pt-8 flex flex-col md:flex-row items-center justify-between gap-4">
          <p className="text-gray-500 text-sm">
            © {new Date().getFullYear()} SubMeter Inc. All rights reserved.
          </p>
          <div className="flex gap-4">
            <Link href="/privacy" className="text-gray-500 hover:text-gray-300 text-sm transition-colors">Privacy Policy</Link>
            <Link href="/terms" className="text-gray-500 hover:text-gray-300 text-sm transition-colors">Terms of Service</Link>
          </div>
        </div>
      </div>
    </footer>
  );
}
