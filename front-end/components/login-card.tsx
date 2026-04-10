"use client";

import { motion } from "framer-motion";
import { useState } from "react";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Layers, Mail, Lock, ArrowRight, Loader2 } from "lucide-react";
import { Field, FieldGroup, FieldLabel } from "@/components/ui/field";
import api from "@/lib/api";

interface LoginCardProps {
  onSignIn: () => void;
  isAnimating: boolean;
}

export function LoginCard({ onSignIn, isAnimating }: LoginCardProps) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setIsLoading(true);

    try {
      // POST to Java Spring Boot backend using Axios
      const response = await api.post("/auth/login", { email, password });

      // Extract JWT token (supports both 'token' and 'accessToken' field names)
      const token = response.data.token || response.data.accessToken;

      if (token) {
        // Store JWT in localStorage
        localStorage.setItem("jwt_token", token);
        setIsLoading(false);

        // Trigger the door animation and redirect (handled by parent)
        onSignIn();
      } else {
        throw new Error("No token received");
      }
    } catch (err) {
      setIsLoading(false);
      // Handle 401/403 or network errors
      if (err && typeof err === "object" && "response" in err) {
        const axiosError = err as { response?: { status?: number } };
        if (axiosError.response?.status === 401 || axiosError.response?.status === 403) {
          setError("Invalid email or password");
        } else {
          setError("An error occurred. Please try again.");
        }
      } else {
        setError("An unexpected error occurred");
      }
    }
  };

  return (
    <motion.div
      initial={{ opacity: 1, scale: 1, y: 0 }}
      animate={{
        opacity: isAnimating ? 0 : 1,
        scale: isAnimating ? 0.95 : 1,
        y: isAnimating ? -20 : 0,
      }}
      transition={{
        duration: 0.5,
        ease: [0.22, 1, 0.36, 1],
      }}
      className="relative w-full max-w-md"
    >
      {/* Glassmorphism card */}
      <div className="relative rounded-2xl bg-slate-900/60 backdrop-blur-2xl border border-slate-700/50 shadow-2xl shadow-black/20 overflow-hidden">
        {/* Subtle gradient overlay */}
        <div className="absolute inset-0 bg-gradient-to-br from-slate-800/20 via-transparent to-slate-900/20" />
        
        {/* Top highlight */}
        <div className="absolute top-0 left-1/2 -translate-x-1/2 w-3/4 h-px bg-gradient-to-r from-transparent via-slate-500/50 to-transparent" />

        <div className="relative z-10 p-8 sm:p-10">
          {/* Logo and branding */}
          <div className="text-center mb-8">
            <motion.div
              initial={{ scale: 0.8, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              transition={{ delay: 0.1 }}
              className="inline-flex items-center justify-center w-14 h-14 rounded-2xl bg-gradient-to-br from-slate-700 to-slate-800 border border-slate-600/50 shadow-lg mb-4"
            >
              <Layers className="w-7 h-7 text-slate-200" />
            </motion.div>
            
            <motion.h1
              initial={{ y: 10, opacity: 0 }}
              animate={{ y: 0, opacity: 1 }}
              transition={{ delay: 0.15 }}
              className="text-2xl font-semibold text-slate-100 tracking-tight"
            >
              MindOps
            </motion.h1>
            
            <motion.p
              initial={{ y: 10, opacity: 0 }}
              animate={{ y: 0, opacity: 1 }}
              transition={{ delay: 0.2 }}
              className="text-sm text-slate-400 mt-2"
            >
              Sign in to access your cloud console
            </motion.p>
          </div>

          {/* Login form */}
          <motion.form
            initial={{ y: 20, opacity: 0 }}
            animate={{ y: 0, opacity: 1 }}
            transition={{ delay: 0.25 }}
            onSubmit={handleSubmit}
            className="space-y-5"
          >
            <FieldGroup>
              <Field>
                <FieldLabel className="text-slate-300 text-sm">Email</FieldLabel>
                <div className="relative">
                  <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-500" />
                  <Input
                    type="email"
                    placeholder="you@company.com"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    className="pl-10 bg-slate-800/50 border-slate-700/50 text-slate-200 placeholder:text-slate-500 focus:border-slate-500 focus:ring-slate-500/20 h-11"
                    required
                  />
                </div>
              </Field>

              <Field>
                <FieldLabel className="text-slate-300 text-sm">Password</FieldLabel>
                <div className="relative">
                  <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-500" />
                  <Input
                    type="password"
                    placeholder="Enter your password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    className="pl-10 bg-slate-800/50 border-slate-700/50 text-slate-200 placeholder:text-slate-500 focus:border-slate-500 focus:ring-slate-500/20 h-11"
                    required
                  />
                </div>
              </Field>
            </FieldGroup>

            <Button
              type="submit"
              disabled={isLoading}
              className="w-full h-11 bg-slate-100 hover:bg-white text-slate-900 font-medium transition-all duration-200 shadow-lg shadow-slate-100/10 hover:shadow-slate-100/20 group"
            >
              {isLoading ? (
                <Loader2 className="w-4 h-4 animate-spin" />
              ) : (
                <>
                  Sign In to Console
                  <ArrowRight className="w-4 h-4 ml-2 transition-transform group-hover:translate-x-0.5" />
                </>
              )}
            </Button>

            {/* Error message */}
            {error && (
              <motion.div
                initial={{ opacity: 0, y: -10 }}
                animate={{ opacity: 1, y: 0 }}
                className="flex items-center justify-center gap-2 p-3 rounded-lg bg-red-500/10 border border-red-500/20"
              >
                <div className="w-1.5 h-1.5 rounded-full bg-red-500" />
                <p className="text-sm text-red-400">{error}</p>
              </motion.div>
            )}
          </motion.form>

          {/* Footer links */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 0.35 }}
            className="mt-6 text-center"
          >
            <a
              href="#"
              className="text-sm text-slate-500 hover:text-slate-300 transition-colors"
            >
              Forgot password?
            </a>
          </motion.div>

          {/* Divider */}
          <div className="flex items-center gap-4 my-6">
            <div className="flex-1 h-px bg-slate-700/50" />
            <span className="text-xs text-slate-500 uppercase tracking-wider">or</span>
            <div className="flex-1 h-px bg-slate-700/50" />
          </div>

          {/* SSO options */}
          <motion.div
            initial={{ y: 10, opacity: 0 }}
            animate={{ y: 0, opacity: 1 }}
            transition={{ delay: 0.4 }}
            className="space-y-3"
          >
            <Button
              type="button"
              variant="outline"
              className="w-full h-10 border-slate-700/50 bg-slate-800/30 text-slate-300 hover:bg-slate-800/50 hover:text-slate-200 hover:border-slate-600"
            >
              Continue with SSO
            </Button>
          </motion.div>

          {/* Sign up link */}
          <motion.p
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 0.45 }}
            className="mt-6 text-center text-sm text-slate-500"
          >
            {"Don't have an account? "}
            <a href="/register" className="text-slate-300 hover:text-white transition-colors font-medium">
              Request access
            </a>
          </motion.p>
        </div>
      </div>

      {/* Glow effect */}
      <div className="absolute -inset-1 bg-gradient-to-r from-slate-600/10 via-slate-500/10 to-slate-600/10 rounded-2xl blur-xl opacity-50 -z-10" />
    </motion.div>
  );
}
