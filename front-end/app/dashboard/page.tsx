"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { motion } from "framer-motion";
import { 
  Layers, 
  Cloud, 
  Activity, 
  Shield, 
  Settings, 
  LogOut,
  Server,
  Database,
  Globe,
  TrendingUp,
  CheckCircle
} from "lucide-react";
import { Button } from "@/components/ui/button";

export default function DashboardPage() {
  const router = useRouter();
  const [userEmail, setUserEmail] = useState<string | null>(null);

  useEffect(() => {
    // Check for JWT token on mount
    const token = localStorage.getItem("jwt_token");
    if (!token) {
      router.push("/login");
      return;
    }
    
    // In a real app, you'd decode the JWT or fetch user data
    // For now, we'll show a placeholder
    setUserEmail("user@mindops.io");
  }, [router]);

  const handleLogout = () => {
    localStorage.removeItem("jwt_token");
    // router.push("/login");
    router.push("/");
  };

  return (
    <main className="min-h-screen bg-slate-950 text-white">
      {/* Background gradient */}
      <div className="fixed inset-0 bg-gradient-to-br from-slate-950 via-slate-900 to-slate-950 -z-10" />
      
      {/* Subtle grid */}
      <div
        className="fixed inset-0 opacity-[0.02] -z-10"
        style={{
          backgroundImage: `
            linear-gradient(rgba(255,255,255,0.1) 1px, transparent 1px),
            linear-gradient(90deg, rgba(255,255,255,0.1) 1px, transparent 1px)
          `,
          backgroundSize: "60px 60px",
        }}
      />

      {/* Header */}
      <header className="border-b border-slate-800/50 bg-slate-950/80 backdrop-blur-xl sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-6 py-4 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-slate-700 to-slate-800 border border-slate-600/50 flex items-center justify-center">
              <Layers className="w-5 h-5 text-slate-200" />
            </div>
            <div>
              <h1 className="text-lg font-semibold text-slate-100">MindOps Console</h1>
              <p className="text-xs text-slate-500">Multi-Cloud Orchestration</p>
            </div>
          </div>
          
          <div className="flex items-center gap-4">
            <span className="text-sm text-slate-400 hidden sm:inline">{userEmail}</span>
            <Button
              variant="ghost"
              size="sm"
              onClick={handleLogout}
              className="text-slate-400 hover:text-white hover:bg-slate-800"
            >
              <LogOut className="w-4 h-4 mr-2" />
              Sign Out
            </Button>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <div className="max-w-7xl mx-auto px-6 py-8">
        {/* Welcome Banner */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="mb-8 p-8 rounded-2xl bg-gradient-to-br from-slate-800/50 to-slate-900/50 border border-slate-700/50 backdrop-blur-sm"
        >
          <div className="flex items-center gap-3 mb-4">
            <div className="w-3 h-3 rounded-full bg-emerald-500 animate-pulse" />
            <span className="text-sm text-emerald-400 font-medium">Connected</span>
          </div>
          <h2 className="text-3xl font-bold text-slate-100 mb-2">
            Welcome to MindOps Console
          </h2>
          <p className="text-slate-400 max-w-2xl">
            Your unified command center for multi-cloud infrastructure management. 
            Monitor, deploy, and scale across AWS, Azure, and GCP from a single dashboard.
          </p>
        </motion.div>

        {/* Quick Stats */}
        <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
          {[
            { label: "Active Instances", value: "24", icon: Server, color: "text-blue-400", trend: "+3" },
            { label: "Databases", value: "8", icon: Database, color: "text-orange-400", trend: "+1" },
            { label: "Regions", value: "12", icon: Globe, color: "text-emerald-400", trend: "0" },
            { label: "Uptime", value: "99.9%", icon: TrendingUp, color: "text-slate-300", trend: "" },
          ].map((stat, index) => (
            <motion.div
              key={stat.label}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.1 + index * 0.05 }}
              className="p-5 rounded-xl bg-slate-900/50 border border-slate-800/50"
            >
              <div className="flex items-center justify-between mb-3">
                <stat.icon className={`w-5 h-5 ${stat.color}`} />
                {stat.trend && (
                  <span className="text-xs text-emerald-400">{stat.trend}</span>
                )}
              </div>
              <p className="text-2xl font-bold text-slate-100">{stat.value}</p>
              <p className="text-sm text-slate-500">{stat.label}</p>
            </motion.div>
          ))}
        </div>

        {/* Cloud Providers Status */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.3 }}
          className="mb-8"
        >
          <h3 className="text-lg font-semibold text-slate-200 mb-4">Cloud Providers</h3>
          <div className="grid sm:grid-cols-3 gap-4">
            {[
              { name: "AWS", services: 12, status: "Healthy", color: "orange" },
              { name: "Azure", services: 8, status: "Healthy", color: "blue" },
              { name: "Azure (Secondary)", services: 4, status: "Healthy", color: "blue" },
            ].map((provider, index) => (
              <div
                key={provider.name + index}
                className={`p-5 rounded-xl bg-slate-900/50 border border-${provider.color}-500/20`}
              >
                <div className="flex items-center justify-between mb-4">
                  <div className="flex items-center gap-2">
                    <Cloud className={`w-5 h-5 text-${provider.color}-400`} />
                    <span className="font-medium text-slate-200">{provider.name}</span>
                  </div>
                  <div className="flex items-center gap-1.5">
                    <CheckCircle className="w-4 h-4 text-emerald-500" />
                    <span className="text-xs text-emerald-400">{provider.status}</span>
                  </div>
                </div>
                <p className="text-sm text-slate-500">
                  {provider.services} active services
                </p>
              </div>
            ))}
          </div>
        </motion.div>

        {/* Quick Actions */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.4 }}
        >
          <h3 className="text-lg font-semibold text-slate-200 mb-4">Quick Actions</h3>
          <div className="flex flex-wrap gap-3">
            <Button variant="outline" className="border-slate-700 bg-slate-800/50 text-slate-200 hover:bg-slate-800">
              <Server className="w-4 h-4 mr-2" />
              Deploy Instance
            </Button>
            <Button variant="outline" className="border-slate-700 bg-slate-800/50 text-slate-200 hover:bg-slate-800">
              <Activity className="w-4 h-4 mr-2" />
              View Metrics
            </Button>
            <Button variant="outline" className="border-slate-700 bg-slate-800/50 text-slate-200 hover:bg-slate-800">
              <Shield className="w-4 h-4 mr-2" />
              Security Audit
            </Button>
            <Button variant="outline" className="border-slate-700 bg-slate-800/50 text-slate-200 hover:bg-slate-800">
              <Settings className="w-4 h-4 mr-2" />
              Settings
            </Button>
          </div>
        </motion.div>
      </div>
    </main>
  );
}
