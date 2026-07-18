import { Navbar } from "@/components/navbar";
import { Footer } from "@/components/footer";
import { Button } from "@/components/ui/button";
import { Check } from "lucide-react";
import Link from "next/link";

export default function PricingPage() {
  return (
    <div className="min-h-screen flex flex-col bg-gray-950 selection:bg-emerald-500/30 selection:text-emerald-200">
      <Navbar />

      <main className="flex-1 pb-24">
        {/* Header */}
        <section className="pt-24 pb-12 md:pt-32 md:pb-20 text-center px-4">
          <h1 className="text-4xl md:text-6xl font-semibold tracking-tight text-white mb-6">
            Simple, transparent pricing
          </h1>
          <p className="text-xl text-gray-400 max-w-2xl mx-auto">
            Start for free, upgrade when you need to. No hidden fees, no surprises.
          </p>
        </section>

        {/* Pricing Cards */}
        <section className="container mx-auto px-4 md:px-8 max-w-6xl">
          <div className="grid md:grid-cols-3 gap-8 items-start">
            
            {/* Starter Plan */}
            <div className="card p-8 rounded-3xl border border-gray-800 bg-gray-900/40 relative">
              <h3 className="text-xl font-medium text-white mb-2">Starter</h3>
              <p className="text-gray-400 text-sm mb-6 h-10">Perfect for side projects and early-stage startups.</p>
              <div className="mb-6">
                <span className="text-5xl font-semibold text-white">₹0</span>
                <span className="text-gray-500 ml-2">/mo</span>
              </div>
              <Link href="/login" className="block w-full mb-8">
                <Button variant="outline" className="w-full h-12 rounded-xl border-gray-700 bg-transparent hover:bg-gray-800 text-white">
                  Get Started
                </Button>
              </Link>
              <div className="space-y-4">
                <div className="text-sm font-medium text-white mb-4">What's included:</div>
                {[
                  "Up to 100 active subscriptions",
                  "Basic flat-rate billing",
                  "Standard checkout page",
                  "Community support",
                  "7-day data retention"
                ].map((feature, i) => (
                  <div key={i} className="flex items-center gap-3">
                    <Check className="h-4 w-4 text-emerald-500 flex-shrink-0" />
                    <span className="text-sm text-gray-300">{feature}</span>
                  </div>
                ))}
              </div>
            </div>

            {/* Pro Plan */}
            <div className="card p-8 rounded-3xl border border-emerald-500/50 bg-gray-900/80 relative transform md:-translate-y-4 shadow-[0_0_40px_-10px_rgba(16,185,129,0.2)]">
              <div className="absolute top-0 left-1/2 -translate-x-1/2 -translate-y-1/2">
                <div className="bg-gradient-to-r from-emerald-400 to-emerald-600 text-white text-xs font-bold uppercase tracking-wider py-1 px-4 rounded-full shadow-lg">
                  Most Popular
                </div>
              </div>
              <h3 className="text-xl font-medium text-white mb-2">Pro</h3>
              <p className="text-gray-400 text-sm mb-6 h-10">For growing businesses that need usage-based billing.</p>
              <div className="mb-6">
                <span className="text-5xl font-semibold text-white">₹2,999</span>
                <span className="text-gray-500 ml-2">/mo</span>
              </div>
              <Link href="/login" className="block w-full mb-8">
                <Button className="w-full h-12 rounded-xl bg-emerald-500 hover:bg-emerald-600 text-gray-950 font-medium">
                  Start 14-day trial
                </Button>
              </Link>
              <div className="space-y-4">
                <div className="text-sm font-medium text-white mb-4">Everything in Starter, plus:</div>
                {[
                  "Unlimited subscriptions",
                  "Usage-based & metered billing",
                  "Custom checkout domains",
                  "Advanced analytics & MRR reporting",
                  "Priority email support",
                  "1-year data retention"
                ].map((feature, i) => (
                  <div key={i} className="flex items-center gap-3">
                    <Check className="h-4 w-4 text-emerald-500 flex-shrink-0" />
                    <span className="text-sm text-gray-300">{feature}</span>
                  </div>
                ))}
              </div>
            </div>

            {/* Enterprise Plan */}
            <div className="card p-8 rounded-3xl border border-gray-800 bg-gray-900/40 relative">
              <h3 className="text-xl font-medium text-white mb-2">Enterprise</h3>
              <p className="text-gray-400 text-sm mb-6 h-10">Custom solutions for large scale organizations.</p>
              <div className="mb-6">
                <span className="text-5xl font-semibold text-white">Custom</span>
              </div>
              <Link href="/contact" className="block w-full mb-8">
                <Button variant="outline" className="w-full h-12 rounded-xl border-gray-700 bg-transparent hover:bg-gray-800 text-white">
                  Contact Sales
                </Button>
              </Link>
              <div className="space-y-4">
                <div className="text-sm font-medium text-white mb-4">Everything in Pro, plus:</div>
                {[
                  "Custom SLAs & 99.99% uptime",
                  "Dedicated success manager",
                  "On-premise deployment options",
                  "Custom integration engineering",
                  "24/7 phone support",
                  "Unlimited data retention"
                ].map((feature, i) => (
                  <div key={i} className="flex items-center gap-3">
                    <Check className="h-4 w-4 text-emerald-500 flex-shrink-0" />
                    <span className="text-sm text-gray-300">{feature}</span>
                  </div>
                ))}
              </div>
            </div>

          </div>
        </section>
        
        {/* FAQ Preview or Note */}
        <section className="container mx-auto px-4 mt-24 max-w-3xl text-center">
          <p className="text-gray-400">
            Have questions about pricing? <Link href="/contact" className="text-emerald-400 hover:underline">Contact our sales team</Link> or check out our <Link href="/faq" className="text-emerald-400 hover:underline">FAQ</Link>.
          </p>
        </section>

      </main>

      <Footer />
    </div>
  );
}
