"use client";

import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Menu, X, ArrowRight, Zap } from "lucide-react";
import { useState } from "react";

export function Navbar() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  return (
    <header className="sticky top-0 z-50 w-full border-b border-white/10 bg-gray-950/80 backdrop-blur-md">
      <div className="container mx-auto px-4 md:px-8">
        <div className="flex h-16 items-center justify-between">
          {/* Logo */}
          <div className="flex items-center gap-2">
            <Link href="/" className="flex items-center gap-2 group">
              <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-emerald-500/10 border border-emerald-500/20 group-hover:bg-emerald-500/20 transition-colors">
                <Zap className="h-4 w-4 text-emerald-500" />
              </div>
              <span className="text-xl font-medium tracking-tight text-gray-100 group-hover:text-white transition-colors">
                SubMeter
              </span>
            </Link>
          </div>

          {/* Desktop Nav */}
          <nav className="hidden md:flex items-center gap-8">
            <Link href="/#features" className="text-sm font-medium text-gray-400 hover:text-gray-100 transition-colors">
              Features
            </Link>
            <Link href="/pricing" className="text-sm font-medium text-gray-400 hover:text-gray-100 transition-colors">
              Pricing
            </Link>
            <Link href="/docs" className="text-sm font-medium text-gray-400 hover:text-gray-100 transition-colors">
              Documentation
            </Link>
          </nav>

          {/* Desktop Actions */}
          <div className="hidden md:flex items-center gap-4">
            <Link href="/login" className="text-sm font-medium text-gray-300 hover:text-white transition-colors">
              Sign In
            </Link>
            <Link href="/dashboard">
              <Button className="bg-emerald-500 hover:bg-emerald-600 text-gray-950 font-medium rounded-full px-6 transition-all hover:scale-105">
                Get Started
                <ArrowRight className="ml-1.5 h-4 w-4" />
              </Button>
            </Link>
          </div>

          {/* Mobile Menu Toggle */}
          <button
            className="md:hidden text-gray-400 hover:text-gray-100"
            onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
          >
            {mobileMenuOpen ? <X className="h-6 w-6" /> : <Menu className="h-6 w-6" />}
          </button>
        </div>
      </div>

      {/* Mobile Nav */}
      {mobileMenuOpen && (
        <div className="md:hidden border-b border-white/10 bg-gray-950 px-4 py-6">
          <nav className="flex flex-col gap-4">
            <Link href="/#features" className="text-lg font-medium text-gray-300" onClick={() => setMobileMenuOpen(false)}>
              Features
            </Link>
            <Link href="/pricing" className="text-lg font-medium text-gray-300" onClick={() => setMobileMenuOpen(false)}>
              Pricing
            </Link>
            <Link href="/docs" className="text-lg font-medium text-gray-300" onClick={() => setMobileMenuOpen(false)}>
              Documentation
            </Link>
            <div className="h-px bg-white/10 my-2" />
            <Link href="/login" className="text-lg font-medium text-gray-300" onClick={() => setMobileMenuOpen(false)}>
              Sign In
            </Link>
            <Link href="/dashboard" onClick={() => setMobileMenuOpen(false)}>
              <Button className="w-full justify-center bg-emerald-500 hover:bg-emerald-600 text-gray-950 font-medium rounded-full mt-2">
                Get Started
              </Button>
            </Link>
          </nav>
        </div>
      )}
    </header>
  );
}
