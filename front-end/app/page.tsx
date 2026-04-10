"use client";

import Link from "next/link";
import { motion } from "framer-motion";
import { ArrowRight, Cloud, Shield, Zap, Layers, Globe, Activity } from "lucide-react";
import { Button } from "@/components/ui/button";

export default function LandingPage() {
  return (
    <main className="relative min-h-screen bg-slate-950 overflow-hidden">
      {/* Background gradient */}
      <div className="absolute inset-0 bg-gradient-to-br from-slate-950 via-slate-900 to-slate-950" />
      
      {/* Subtle grid pattern */}
      <div
        className="absolute inset-0 opacity-[0.02]"
        style={{
          backgroundImage: `
            linear-gradient(rgba(255,255,255,0.1) 1px, transparent 1px),
            linear-gradient(90deg, rgba(255,255,255,0.1) 1px, transparent 1px)
          `,
          backgroundSize: "60px 60px",
        }}
      />

      {/* Animated glow orbs */}
      <motion.div
        animate={{
          scale: [1, 1.2, 1],
          opacity: [0.15, 0.25, 0.15],
        }}
        transition={{ duration: 8, repeat: Infinity, ease: "easeInOut" }}
        className="absolute top-0 left-1/4 w-[600px] h-[600px] bg-blue-500/10 rounded-full blur-3xl"
      />
      <motion.div
        animate={{
          scale: [1.2, 1, 1.2],
          opacity: [0.1, 0.2, 0.1],
        }}
        transition={{ duration: 10, repeat: Infinity, ease: "easeInOut" }}
        className="absolute bottom-0 right-1/4 w-[500px] h-[500px] bg-orange-500/10 rounded-full blur-3xl"
      />

      {/* Navigation */}
      <motion.nav
        initial={{ opacity: 0, y: -10 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.1 }}
        className="relative z-10 px-6 py-4 flex items-center justify-between max-w-7xl mx-auto"
      >
        <div className="flex items-center gap-2">
          <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-slate-700 to-slate-800 border border-slate-600/50 flex items-center justify-center">
            <Layers className="w-5 h-5 text-slate-200" />
          </div>
          <span className="text-lg font-semibold text-slate-100">MindOps</span>
        </div>
        
        <div className="hidden sm:flex items-center gap-8">
          <a href="#features" className="text-sm text-slate-400 hover:text-slate-200 transition-colors">Features</a>
          <a href="#" className="text-sm text-slate-400 hover:text-slate-200 transition-colors">Pricing</a>
          <a href="#" className="text-sm text-slate-400 hover:text-slate-200 transition-colors">Docs</a>
        </div>
{/* 
        <Link href="/login">
          <Button variant="outline" size="sm" className="border-slate-700 bg-slate-800/50 text-slate-200 hover:bg-slate-800 hover:text-white">
            Sign In
          </Button>
        </Link>
        <Link href="/register">
          <Button variant="outline" size="sm" className="border-slate-700 bg-slate-800/50 text-slate-200 hover:bg-slate-800 hover:text-white">
            Sign Up
          </Button>
        </Link> */}
        <div className="flex items-center gap-4">
          <Link href="/login">
            <Button variant="outline" size="sm" className="border-slate-700 bg-slate-800/50 text-slate-200 hover:bg-slate-800 hover:text-white">
              Sign In
            </Button>
          </Link>
          
          <Link href="/register">
            {/* Architect Note: I changed the Sign Up button to be solid white so it acts as a strong Call To Action (CTA). Two outline buttons next to each other creates visual confusion. */}
            <Button size="sm" className="bg-slate-100 text-slate-900 hover:bg-white font-medium">
              Sign Up
            </Button>
          </Link>
        </div>
      </motion.nav>

      {/* Hero Section */}
      <div className="relative z-10 flex flex-col items-center justify-center min-h-[calc(100vh-80px)] px-6 text-center">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 }}
          className="mb-6"
        >
          <span className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-slate-800/50 border border-slate-700/50 text-xs text-slate-400">
            <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse" />
            Now supporting multi-region deployments
          </span>
        </motion.div>

        <motion.h1
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.3 }}
          className="text-4xl sm:text-5xl lg:text-6xl font-bold text-slate-100 tracking-tight max-w-4xl leading-tight text-balance"
        >
          Deploy Cloud Infrastructure{" "}
          <span className="text-transparent bg-clip-text bg-gradient-to-r from-blue-400 via-slate-200 to-orange-400">
            at the Speed of Thought
          </span>
        </motion.h1>

        <motion.p
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.4 }}
          className="mt-6 text-lg text-slate-400 max-w-2xl text-pretty"
        >
          Unified multi-cloud orchestration for AWS, Azure, and GCP. 
          Manage infrastructure, automate deployments, and scale with confidence from a single intelligent platform.
        </motion.p>

        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.5 }}
          className="mt-10 flex flex-col sm:flex-row items-center gap-4"
        >
          <Link href="/login">
            <Button size="lg" className="bg-slate-100 hover:bg-white text-slate-900 font-medium px-8 h-12 text-base shadow-lg shadow-slate-100/10 hover:shadow-slate-100/20 group">
              Get Started
              <ArrowRight className="w-4 h-4 ml-2 transition-transform group-hover:translate-x-0.5" />
            </Button>
          </Link>
          <Button variant="outline" size="lg" className="border-slate-700 bg-transparent text-slate-300 hover:bg-slate-800 hover:text-white px-8 h-12 text-base">
            View Documentation
          </Button>
        </motion.div>

        {/* Cloud provider logos */}
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.7 }}
          className="mt-16 flex items-center gap-8"
        >
          <span className="text-xs text-slate-500 uppercase tracking-wider">Supports</span>
          <div className="flex items-center gap-6">
            <div className="flex items-center gap-2 text-orange-400/60">
              <Cloud className="w-5 h-5" />
              <span className="text-sm font-medium">AWS</span>
            </div>
            <div className="flex items-center gap-2 text-blue-400/60">
              <Globe className="w-5 h-5" />
              <span className="text-sm font-medium">Azure</span>
            </div>
            <div className="flex items-center gap-2 text-slate-400/60">
              <Activity className="w-5 h-5" />
              <span className="text-sm font-medium">GCP</span>
            </div>
          </div>
        </motion.div>
      </div>

      {/* Features Section */}
      <section id="features" className="relative z-10 px-6 py-24 max-w-7xl mx-auto">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          className="text-center mb-16"
        >
          <h2 className="text-3xl font-bold text-slate-100 mb-4">
            Enterprise-Grade Cloud Management
          </h2>
          <p className="text-slate-400 max-w-2xl mx-auto">
            Everything you need to orchestrate your multi-cloud infrastructure efficiently and securely.
          </p>
        </motion.div>

        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-6">
          {[
            {
              icon: Shield,
              title: "Security First",
              description: "End-to-end encryption, RBAC, and compliance-ready infrastructure management.",
              color: "text-blue-400",
              bg: "bg-blue-500/10",
              border: "border-blue-500/20",
            },
            {
              icon: Zap,
              title: "Instant Deployments",
              description: "Deploy infrastructure across regions in seconds with intelligent automation.",
              color: "text-orange-400",
              bg: "bg-orange-500/10",
              border: "border-orange-500/20",
            },
            {
              icon: Activity,
              title: "Real-time Monitoring",
              description: "Unified observability dashboard with cost tracking and performance insights.",
              color: "text-emerald-400",
              bg: "bg-emerald-500/10",
              border: "border-emerald-500/20",
            },
          ].map((feature, index) => (
            <motion.div
              key={feature.title}
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ delay: index * 0.1 }}
              className={`p-6 rounded-2xl bg-slate-900/50 border ${feature.border} backdrop-blur-sm`}
            >
              <div className={`w-12 h-12 rounded-xl ${feature.bg} flex items-center justify-center mb-4`}>
                <feature.icon className={`w-6 h-6 ${feature.color}`} />
              </div>
              <h3 className="text-lg font-semibold text-slate-100 mb-2">{feature.title}</h3>
              <p className="text-sm text-slate-400">{feature.description}</p>
            </motion.div>
          ))}
        </div>
      </section>

      {/* Footer */}
      <footer className="relative z-10 px-6 py-8 border-t border-slate-800/50">
        <div className="max-w-7xl mx-auto flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-2 text-slate-500 text-sm">
            <Layers className="w-4 h-4" />
            <span>MindOps</span>
            <span className="mx-2">|</span>
            <span>2026</span>
            <span className="mx-2">|</span>
            <span>v1.0</span>
          </div>
          <div className="flex items-center gap-6 text-sm text-slate-500">
            <a href="#" className="hover:text-slate-300 transition-colors">Privacy</a>
            <a href="#" className="hover:text-slate-300 transition-colors">Terms</a>
            <a href="#" className="hover:text-slate-300 transition-colors">Contact</a>
          </div>
        </div>
      </footer>
    </main>
  );
}
