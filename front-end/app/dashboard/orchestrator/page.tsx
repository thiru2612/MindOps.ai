// "use client";

// import { useEffect, useState, useRef } from "react";
// import {
//   Terminal,
//   ChevronDown,
//   CheckCircle2,
//   AlertCircle,
//   Loader2,
//   Cpu,
//   Cloud,
//   Zap,
//   RefreshCw,
// } from "lucide-react";
// import api from "@/lib/api";

// // ── Types ─────────────────────────────────────────────────────────────────────

// interface Credential {
//   publicId: string;
//   credentialLabel: string;
//   provider: "AWS" | "AZURE";
// }

// interface DeploymentResult {
//   planId: string;
//   status: string;
//   userPrompt: string;
//   costEstimate: number | null;
//   aiGeneratedConfig: Record<string, unknown>;
//   credentialId: string;
//   createdAt: string;
//   updatedAt: string;
// }

// type PageState = "idle" | "loading" | "success" | "error";

// // ── Syntax Highlighting ───────────────────────────────────────────────────────

// function highlightJson(json: string): string {
//   return json
//     .replace(/&/g, "&amp;")
//     .replace(/</g, "&lt;")
//     .replace(/>/g, "&gt;")
//     .replace(
//       /("(\\u[a-zA-Z0-9]{4}|\\[^u]|[^\\"])*"(\s*:)?|\b(true|false|null)\b|-?\d+(?:\.\d*)?(?:[eE][+\-]?\d+)?)/g,
//       (match) => {
//         let cls = "text-amber-300";
//         if (/^"/.test(match)) {
//           cls = /:$/.test(match) ? "text-sky-300" : "text-emerald-300";
//         } else if (/true|false/.test(match)) {
//           cls = "text-violet-400";
//         } else if (/null/.test(match)) {
//           cls = "text-rose-400";
//         }
//         return `<span class="${cls}">${match}</span>`;
//       }
//     );
// }

// // ── Provider Badge ────────────────────────────────────────────────────────────

// function ProviderBadge({ provider }: { provider: "AWS" | "AZURE" }) {
//   const isAws = provider === "AWS";
//   return (
//     <span
//       className={`
//         inline-flex items-center gap-1 px-1.5 py-0.5 rounded text-[10px] font-bold
//         tracking-widest uppercase border
//         ${
//           isAws
//             ? "bg-orange-500/10 text-orange-400 border-orange-500/30"
//             : "bg-sky-500/10 text-sky-400 border-sky-500/30"
//         }
//       `}
//     >
//       {isAws ? "AWS" : "AZ"}
//     </span>
//   );
// }

// // ── Custom Select ─────────────────────────────────────────────────────────────

// function CredentialSelect({
//   credentials,
//   selected,
//   onSelect,
//   disabled,
// }: {
//   credentials: Credential[];
//   selected: string;
//   onSelect: (id: string) => void;
//   disabled: boolean;
// }) {
//   const [open, setOpen] = useState(false);
//   const ref = useRef<HTMLDivElement>(null);

//   const selectedCred = credentials.find((c) => c.publicId === selected);

//   useEffect(() => {
//     function handleClickOutside(e: MouseEvent) {
//       if (ref.current && !ref.current.contains(e.target as Node)) {
//         setOpen(false);
//       }
//     }
//     document.addEventListener("mousedown", handleClickOutside);
//     return () => document.removeEventListener("mousedown", handleClickOutside);
//   }, []);

//   return (
//     <div ref={ref} className="relative">
//       <button
//         type="button"
//         disabled={disabled}
//         onClick={() => setOpen((prev) => !prev)}
//         className={`
//           w-full flex items-center justify-between gap-3
//           px-4 py-3 rounded-xl border text-left text-sm
//           bg-slate-900/60 backdrop-blur-sm
//           border-slate-700/60 text-slate-200
//           transition-all duration-200
//           focus:outline-none focus:ring-1 focus:ring-sky-500/60 focus:border-sky-500/40
//           hover:border-slate-600/80 hover:bg-slate-900/80
//           disabled:opacity-50 disabled:cursor-not-allowed
//           ${open ? "border-sky-500/40 ring-1 ring-sky-500/30" : ""}
//         `}
//       >
//         <div className="flex items-center gap-3 min-w-0">
//           <div
//             className={`
//             w-7 h-7 rounded-lg flex items-center justify-center flex-shrink-0
//             ${selectedCred ? "bg-sky-500/15 border border-sky-500/30" : "bg-slate-800 border border-slate-700"}
//           `}
//           >
//             <Cloud
//               className={`w-3.5 h-3.5 ${selectedCred ? "text-sky-400" : "text-slate-600"}`}
//             />
//           </div>
//           {selectedCred ? (
//             <div className="flex items-center gap-2 min-w-0">
//               <span className="text-slate-200 font-medium truncate">
//                 {selectedCred.credentialLabel}
//               </span>
//               <ProviderBadge provider={selectedCred.provider} />
//             </div>
//           ) : (
//             <span className="text-slate-500 font-mono text-xs">
//               SELECT CREDENTIAL
//             </span>
//           )}
//         </div>
//         <ChevronDown
//           className={`w-4 h-4 text-slate-500 flex-shrink-0 transition-transform duration-200
//             ${open ? "rotate-180 text-sky-400" : ""}`}
//         />
//       </button>

//       {open && credentials.length > 0 && (
//         <div
//           className="
//             absolute z-50 w-full mt-2 py-1.5 rounded-xl border
//             bg-slate-900/95 backdrop-blur-xl border-slate-700/60
//             shadow-2xl shadow-black/60
//           "
//         >
//           {credentials.map((cred) => (
//             <button
//               key={cred.publicId}
//               type="button"
//               onClick={() => {
//                 onSelect(cred.publicId);
//                 setOpen(false);
//               }}
//               className={`
//                 w-full flex items-center gap-3 px-4 py-2.5 text-sm text-left
//                 transition-colors duration-150
//                 hover:bg-sky-500/10
//                 ${selected === cred.publicId ? "bg-sky-500/10 text-sky-300" : "text-slate-300"}
//               `}
//             >
//               <div className="w-6 h-6 rounded-md bg-slate-800 border border-slate-700 flex items-center justify-center flex-shrink-0">
//                 <Cloud className="w-3 h-3 text-slate-500" />
//               </div>
//               <span className="font-medium truncate">{cred.credentialLabel}</span>
//               <ProviderBadge provider={cred.provider} />
//               {selected === cred.publicId && (
//                 <CheckCircle2 className="w-3.5 h-3.5 ml-auto text-sky-400 flex-shrink-0" />
//               )}
//             </button>
//           ))}
//         </div>
//       )}

//       {open && credentials.length === 0 && (
//         <div className="absolute z-50 w-full mt-2 py-4 rounded-xl border bg-slate-900/95 border-slate-700/60 shadow-2xl text-center text-slate-500 text-sm">
//           No credentials found
//         </div>
//       )}
//     </div>
//   );
// }

// // ── AI Processing Loader ──────────────────────────────────────────────────────

// function AiProcessingLoader() {
//   const steps = [
//     "Parsing natural language prompt...",
//     "Consulting Gemini AI engine...",
//     "Translating to cloud SDK parameters...",
//     "Validating FinOps guardrails...",
//     "Generating deployment blueprint...",
//   ];
//   const [stepIndex, setStepIndex] = useState(0);

//   useEffect(() => {
//     const interval = setInterval(() => {
//       setStepIndex((prev) => (prev + 1) % steps.length);
//     }, 1800);
//     return () => clearInterval(interval);
//   }, []);

//   return (
//     <div className="rounded-2xl border border-sky-500/20 bg-sky-500/5 backdrop-blur-sm p-8">
//       <div className="flex flex-col items-center gap-6">
//         {/* Central animated icon */}
//         <div className="relative">
//           <div className="absolute inset-0 rounded-full bg-sky-500/20 animate-ping" />
//           <div className="relative w-14 h-14 rounded-full bg-slate-900 border border-sky-500/40 flex items-center justify-center">
//             <Cpu className="w-6 h-6 text-sky-400 animate-pulse" />
//           </div>
//         </div>

//         {/* Label */}
//         <div className="text-center">
//           <p className="text-sky-300 font-semibold tracking-wide text-sm uppercase">
//             AI Processing
//           </p>
//           <p className="text-slate-500 text-xs mt-1 font-mono">
//             gemini-pro engaged
//           </p>
//         </div>

//         {/* Scrolling step indicator */}
//         <div className="w-full max-w-sm h-6 overflow-hidden relative">
//           <p
//             key={stepIndex}
//             className="text-slate-400 text-xs text-center font-mono animate-fade-in"
//           >
//             {steps[stepIndex]}
//           </p>
//         </div>

//         {/* Progress bar */}
//         <div className="w-full max-w-sm h-px bg-slate-800 rounded-full overflow-hidden">
//           <div className="h-full bg-gradient-to-r from-sky-500 via-violet-500 to-sky-500 bg-[length:200%_100%] animate-shimmer rounded-full" />
//         </div>

//         {/* Skeleton lines */}
//         <div className="w-full space-y-2">
//           {[80, 60, 90, 45].map((width, i) => (
//             <div
//               key={i}
//               className="h-2 rounded-full bg-slate-800/80 animate-pulse"
//               style={{
//                 width: `${width}%`,
//                 animationDelay: `${i * 150}ms`,
//               }}
//             />
//           ))}
//         </div>
//       </div>
//     </div>
//   );
// }

// // ── Result Panel ──────────────────────────────────────────────────────────────

// function ResultPanel({ result }: { result: DeploymentResult }) {
//   const [copied, setCopied] = useState(false);
//   const formattedJson = JSON.stringify(result, null, 2);
//   const highlighted = highlightJson(formattedJson);

//   function handleCopy() {
//     navigator.clipboard.writeText(formattedJson).then(() => {
//       setCopied(true);
//       setTimeout(() => setCopied(false), 2000);
//     });
//   }

//   return (
//     <div className="space-y-4 animate-fade-in">
//       {/* Success Banner */}
//       <div className="flex items-start gap-4 p-4 rounded-xl border border-emerald-500/25 bg-emerald-500/8 backdrop-blur-sm">
//         <div className="w-8 h-8 rounded-lg bg-emerald-500/15 border border-emerald-500/30 flex items-center justify-center flex-shrink-0 mt-0.5">
//           <CheckCircle2 className="w-4 h-4 text-emerald-400" />
//         </div>
//         <div className="flex-1 min-w-0">
//           <p className="text-emerald-300 font-semibold text-sm">
//             Blueprint Generated
//           </p>
//           <p className="text-slate-400 text-xs mt-0.5 font-mono">
//             Plan ID: {result.planId} &nbsp;·&nbsp; Status:{" "}
//             <span className="text-amber-400">{result.status}</span>
//             {result.costEstimate !== null && (
//               <>
//                 &nbsp;·&nbsp; Est. Cost:{" "}
//                 <span className="text-emerald-400">
//                   ${result.costEstimate}/mo
//                 </span>
//               </>
//             )}
//           </p>
//         </div>
//       </div>

//       {/* Code Block */}
//       <div className="rounded-xl border border-slate-700/60 overflow-hidden">
//         {/* Code block header bar */}
//         <div className="flex items-center justify-between px-4 py-2.5 bg-slate-900/80 border-b border-slate-700/60">
//           <div className="flex items-center gap-2">
//             <div className="flex gap-1.5">
//               <div className="w-2.5 h-2.5 rounded-full bg-rose-500/60" />
//               <div className="w-2.5 h-2.5 rounded-full bg-amber-500/60" />
//               <div className="w-2.5 h-2.5 rounded-full bg-emerald-500/60" />
//             </div>
//             <span className="text-slate-500 text-xs font-mono ml-2">
//               deployment_blueprint.json
//             </span>
//           </div>
//           <button
//             onClick={handleCopy}
//             className="flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-mono
//               text-slate-400 hover:text-slate-200 hover:bg-slate-700/60
//               border border-transparent hover:border-slate-600/60
//               transition-all duration-150"
//           >
//             {copied ? (
//               <>
//                 <CheckCircle2 className="w-3 h-3 text-emerald-400" />
//                 <span className="text-emerald-400">Copied</span>
//               </>
//             ) : (
//               <>
//                 <svg
//                   className="w-3 h-3"
//                   viewBox="0 0 24 24"
//                   fill="none"
//                   stroke="currentColor"
//                   strokeWidth={2}
//                 >
//                   <rect x="9" y="9" width="13" height="13" rx="2" ry="2" />
//                   <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1" />
//                 </svg>
//                 Copy JSON
//               </>
//             )}
//           </button>
//         </div>

//         {/* Syntax-highlighted JSON */}
//         <pre className="p-5 overflow-x-auto text-xs leading-relaxed bg-slate-950/80 max-h-[520px] overflow-y-auto scrollbar-thin scrollbar-track-slate-900 scrollbar-thumb-slate-700">
//           <code
//             className="font-mono"
//             dangerouslySetInnerHTML={{ __html: highlighted }}
//           />
//         </pre>
//       </div>
//     </div>
//   );
// }

// // ── Error Panel ───────────────────────────────────────────────────────────────

// function ErrorPanel({
//   message,
//   onRetry,
// }: {
//   message: string;
//   onRetry: () => void;
// }) {
//   return (
//     <div className="flex items-start gap-4 p-4 rounded-xl border border-rose-500/25 bg-rose-500/8 animate-fade-in">
//       <div className="w-8 h-8 rounded-lg bg-rose-500/15 border border-rose-500/30 flex items-center justify-center flex-shrink-0 mt-0.5">
//         <AlertCircle className="w-4 h-4 text-rose-400" />
//       </div>
//       <div className="flex-1 min-w-0">
//         <p className="text-rose-300 font-semibold text-sm">Execution Failed</p>
//         <p className="text-slate-400 text-xs mt-0.5 font-mono break-words">
//           {message}
//         </p>
//       </div>
//       <button
//         onClick={onRetry}
//         className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs
//           text-slate-300 hover:text-white bg-slate-800 hover:bg-slate-700
//           border border-slate-700/60 hover:border-slate-600
//           transition-all duration-150 flex-shrink-0"
//       >
//         <RefreshCw className="w-3 h-3" />
//         Retry
//       </button>
//     </div>
//   );
// }

// // ── Main Page ─────────────────────────────────────────────────────────────────

// export default function OrchestratorPage() {
//   const [credentials, setCredentials] = useState<Credential[]>([]);
//   const [selectedCredentialId, setSelectedCredentialId] = useState<string>("");
//   const [prompt, setPrompt] = useState<string>("");
//   const [pageState, setPageState] = useState<PageState>("idle");
//   const [result, setResult] = useState<DeploymentResult | null>(null);
//   const [errorMessage, setErrorMessage] = useState<string>("");
//   const [credentialsLoading, setCredentialsLoading] = useState<boolean>(true);
//   const textareaRef = useRef<HTMLTextAreaElement>(null);

//   const isFormValid = selectedCredentialId !== "" && prompt.trim().length >= 10;
//   const isSubmitting = pageState === "loading";

//   // ── Load credentials on mount ─────────────────────────────────────────────

//   useEffect(() => {
//     async function fetchCredentials() {
//       setCredentialsLoading(true);
//       try {
//         const response = await api.get<{ credentials: Credential[] }>(
//           "/credentials"
//         );
//         const list = response.data.credentials ?? [];
//         setCredentials(list);
//         if (list.length === 1) {
//           setSelectedCredentialId(list[0].publicId);
//         }
//       } catch (err) {
//         console.error("[Orchestrator] Failed to load credentials:", err);
//         setCredentials([]);
//       } finally {
//         setCredentialsLoading(false);
//       }
//     }
//     fetchCredentials();
//   }, []);

//   // ── Auto-resize textarea ──────────────────────────────────────────────────

//   useEffect(() => {
//     const textarea = textareaRef.current;
//     if (!textarea) return;
//     textarea.style.height = "auto";
//     textarea.style.height = `${Math.max(textarea.scrollHeight, 140)}px`;
//   }, [prompt]);

//   // ── Submit handler ────────────────────────────────────────────────────────

//   async function handleDeploy() {
//     if (!isFormValid || isSubmitting) return;

//     setPageState("loading");
//     setResult(null);
//     setErrorMessage("");

//     try {
//       const response = await api.post<DeploymentResult>(
//         "/api/v1/deployments/generate",
//         {
//           prompt: prompt.trim(),
//           credentialId: selectedCredentialId,
//         }
//       );
//       setResult(response.data);
//       setPageState("success");
//     } catch (err: unknown) {
//       const message =
//         (err as { response?: { data?: { message?: string } }; message?: string })
//           ?.response?.data?.message ??
//         (err as { message?: string })?.message ??
//         "An unexpected error occurred. Please try again.";
//       setErrorMessage(message);
//       setPageState("error");
//     }
//   }

//   function handleReset() {
//     setPageState("idle");
//     setResult(null);
//     setErrorMessage("");
//   }

//   function handleKeyDown(e: React.KeyboardEvent<HTMLTextAreaElement>) {
//     if ((e.metaKey || e.ctrlKey) && e.key === "Enter") {
//       e.preventDefault();
//       handleDeploy();
//     }
//   }

//   // ── Selected credential display ───────────────────────────────────────────

//   const selectedCred = credentials.find(
//     (c) => c.publicId === selectedCredentialId
//   );

//   // ── Render ────────────────────────────────────────────────────────────────

//   return (
//     <div className="min-h-screen bg-slate-950 text-slate-100">
//       {/* Ambient background grid */}
//       <div
//         className="fixed inset-0 opacity-[0.015] pointer-events-none"
//         style={{
//           backgroundImage:
//             "linear-gradient(rgba(148,163,184,1) 1px, transparent 1px), linear-gradient(90deg, rgba(148,163,184,1) 1px, transparent 1px)",
//           backgroundSize: "48px 48px",
//         }}
//       />

//       {/* Ambient glow */}
//       <div className="fixed top-0 left-1/2 -translate-x-1/2 w-[800px] h-[400px] bg-sky-500/4 rounded-full blur-3xl pointer-events-none" />

//       <div className="relative max-w-4xl mx-auto px-6 py-10 space-y-8">
//         {/* ── Header ──────────────────────────────────────────────────────── */}
//         <div className="flex items-start justify-between">
//           <div className="space-y-1">
//             <div className="flex items-center gap-3">
//               <div className="w-9 h-9 rounded-xl bg-sky-500/15 border border-sky-500/30 flex items-center justify-center">
//                 <Zap className="w-4.5 h-4.5 text-sky-400" />
//               </div>
//               <h1 className="text-2xl font-bold tracking-tight text-white">
//                 AI Orchestrator
//               </h1>
//             </div>
//             <p className="text-slate-500 text-sm pl-12">
//               Natural language to infrastructure.
//             </p>
//           </div>

//           {/* Live status pill */}
//           <div className="flex items-center gap-2 px-3 py-1.5 rounded-full border border-slate-700/60 bg-slate-900/60 backdrop-blur-sm">
//             <div
//               className={`w-1.5 h-1.5 rounded-full ${
//                 isSubmitting
//                   ? "bg-amber-400 animate-pulse"
//                   : pageState === "success"
//                     ? "bg-emerald-400"
//                     : pageState === "error"
//                       ? "bg-rose-400"
//                       : "bg-slate-600"
//               }`}
//             />
//             <span className="text-xs font-mono text-slate-400 uppercase tracking-widest">
//               {isSubmitting
//                 ? "executing"
//                 : pageState === "success"
//                   ? "deployed"
//                   : pageState === "error"
//                     ? "failed"
//                     : "standby"}
//             </span>
//           </div>
//         </div>

//         {/* ── Configuration Panel ──────────────────────────────────────────── */}
//         <div className="rounded-2xl border border-slate-700/50 bg-slate-900/40 backdrop-blur-sm p-6 space-y-5">
//           <div className="flex items-center gap-2 mb-1">
//             <Cloud className="w-3.5 h-3.5 text-slate-500" />
//             <span className="text-xs font-mono text-slate-500 uppercase tracking-widest">
//               Target Credential
//             </span>
//           </div>

//           {credentialsLoading ? (
//             <div className="h-12 rounded-xl bg-slate-800/60 border border-slate-700/40 animate-pulse" />
//           ) : (
//             <CredentialSelect
//               credentials={credentials}
//               selected={selectedCredentialId}
//               onSelect={setSelectedCredentialId}
//               disabled={isSubmitting}
//             />
//           )}

//           {credentials.length === 0 && !credentialsLoading && (
//             <p className="text-xs text-amber-400/80 font-mono flex items-center gap-2">
//               <AlertCircle className="w-3.5 h-3.5" />
//               No credentials configured. Add cloud credentials in the Vault
//               first.
//             </p>
//           )}

//           {selectedCred && (
//             <div className="flex items-center gap-2 text-xs font-mono text-slate-600">
//               <div className="w-1 h-1 rounded-full bg-slate-700" />
//               <span>
//                 ID: {selectedCred.publicId}
//               </span>
//               <div className="w-1 h-1 rounded-full bg-slate-700" />
//               <span className="uppercase">{selectedCred.provider}</span>
//             </div>
//           )}
//         </div>

//         {/* ── Command Panel ────────────────────────────────────────────────── */}
//         <div className="rounded-2xl border border-slate-700/50 bg-slate-900/40 backdrop-blur-sm overflow-hidden">
//           {/* Terminal header bar */}
//           <div className="flex items-center justify-between px-5 py-3 border-b border-slate-700/50 bg-slate-900/60">
//             <div className="flex items-center gap-3">
//               <div className="flex gap-1.5">
//                 <div className="w-2.5 h-2.5 rounded-full bg-rose-500/50" />
//                 <div className="w-2.5 h-2.5 rounded-full bg-amber-500/50" />
//                 <div className="w-2.5 h-2.5 rounded-full bg-emerald-500/50" />
//               </div>
//               <div className="flex items-center gap-2">
//                 <Terminal className="w-3.5 h-3.5 text-slate-500" />
//                 <span className="text-xs font-mono text-slate-500">
//                   mindops@orchestrator:~$
//                 </span>
//               </div>
//             </div>
//             <span className="text-xs font-mono text-slate-600">
//               ⌘ + Enter to deploy
//             </span>
//           </div>

//           {/* Prompt textarea */}
//           <div className="relative">
//             <div className="absolute top-4 left-5 text-slate-600 font-mono text-sm select-none pointer-events-none">
//               &gt;
//             </div>
//             <textarea
//               ref={textareaRef}
//               value={prompt}
//               onChange={(e) => setPrompt(e.target.value)}
//               onKeyDown={handleKeyDown}
//               disabled={isSubmitting}
//               placeholder="E.g., Deploy a highly available Ubuntu EC2 instance in us-east-1 with auto-scaling between 2 and 10 nodes, an Application Load Balancer, and a t3.medium instance type..."
//               className="
//                 w-full pl-10 pr-5 py-4 min-h-[140px]
//                 bg-transparent resize-none
//                 font-mono text-sm text-slate-200 leading-relaxed
//                 placeholder:text-slate-700
//                 focus:outline-none
//                 disabled:opacity-50 disabled:cursor-not-allowed
//                 transition-colors duration-200
//               "
//               style={{ height: "auto" }}
//             />
//           </div>

//           {/* Char count + submit row */}
//           <div className="flex items-center justify-between px-5 py-3 border-t border-slate-800/60 bg-slate-950/30">
//             <div className="flex items-center gap-3">
//               <span
//                 className={`text-xs font-mono ${
//                   prompt.length > 1800
//                     ? "text-rose-400"
//                     : prompt.length > 1200
//                       ? "text-amber-400"
//                       : "text-slate-600"
//                 }`}
//               >
//                 {prompt.length} / 2000
//               </span>
//               {prompt.trim().length > 0 && prompt.trim().length < 10 && (
//                 <span className="text-xs font-mono text-amber-500/70">
//                   prompt too short
//                 </span>
//               )}
//             </div>

//             <button
//               onClick={handleDeploy}
//               disabled={!isFormValid || isSubmitting}
//               className={`
//                 flex items-center gap-2.5 px-5 py-2.5 rounded-xl
//                 text-sm font-semibold
//                 transition-all duration-200
//                 focus:outline-none focus:ring-2 focus:ring-sky-500/40
//                 ${
//                   isFormValid && !isSubmitting
//                     ? `bg-sky-500 hover:bg-sky-400 text-white
//                        shadow-lg shadow-sky-500/25 hover:shadow-sky-400/35
//                        active:scale-95`
//                     : "bg-slate-800 text-slate-600 cursor-not-allowed border border-slate-700/60"
//                 }
//               `}
//             >
//               {isSubmitting ? (
//                 <>
//                   <Loader2 className="w-4 h-4 animate-spin" />
//                   <span>Processing...</span>
//                 </>
//               ) : (
//                 <>
//                   <Terminal className="w-4 h-4" />
//                   <span>Deploy Infrastructure</span>
//                 </>
//               )}
//             </button>
//           </div>
//         </div>

//         {/* ── Output Section ───────────────────────────────────────────────── */}
//         {pageState !== "idle" && (
//           <div className="space-y-2">
//             {/* Output section label */}
//             <div className="flex items-center justify-between">
//               <div className="flex items-center gap-2">
//                 <div className="w-1 h-4 rounded-full bg-sky-500/60" />
//                 <span className="text-xs font-mono text-slate-500 uppercase tracking-widest">
//                   Execution Output
//                 </span>
//               </div>
//               {(pageState === "success" || pageState === "error") && (
//                 <button
//                   onClick={handleReset}
//                   className="flex items-center gap-1.5 text-xs font-mono text-slate-600
//                     hover:text-slate-400 transition-colors duration-150"
//                 >
//                   <RefreshCw className="w-3 h-3" />
//                   New deployment
//                 </button>
//               )}
//             </div>

//             {/* Loading state */}
//             {pageState === "loading" && <AiProcessingLoader />}

//             {/* Success state */}
//             {pageState === "success" && result && (
//               <ResultPanel result={result} />
//             )}

//             {/* Error state */}
//             {pageState === "error" && (
//               <ErrorPanel message={errorMessage} onRetry={handleReset} />
//             )}
//           </div>
//         )}

//         {/* ── Footer ──────────────────────────────────────────────────────── */}
//         <div className="flex items-center justify-between pt-2 border-t border-slate-800/60">
//           <p className="text-xs font-mono text-slate-700">
//             mindops-cloud · orchestrator v1.0
//           </p>
//           <p className="text-xs font-mono text-slate-700">
//             powered by Gemini AI
//           </p>
//         </div>
//       </div>

//       {/* Global keyframe animations */}
//       <style jsx global>{`
//         @keyframes fade-in {
//           from {
//             opacity: 0;
//             transform: translateY(6px);
//           }
//           to {
//             opacity: 1;
//             transform: translateY(0);
//           }
//         }
//         @keyframes shimmer {
//           0% {
//             background-position: 200% center;
//           }
//           100% {
//             background-position: -200% center;
//           }
//         }
//         .animate-fade-in {
//           animation: fade-in 0.3s ease-out both;
//         }
//         .animate-shimmer {
//           animation: shimmer 2s linear infinite;
//         }
//         .scrollbar-thin::-webkit-scrollbar {
//           width: 4px;
//           height: 4px;
//         }
//         .scrollbar-track-slate-900::-webkit-scrollbar-track {
//           background: rgb(15 23 42);
//         }
//         .scrollbar-thumb-slate-700::-webkit-scrollbar-thumb {
//           background: rgb(51 65 85);
//           border-radius: 9999px;
//         }
//       `}</style>
//     </div>
//   );
// }


// "use client";

// import { useEffect, useState, useRef } from "react";
// import {
//   Terminal,
//   ChevronDown,
//   CheckCircle2,
//   AlertCircle,
//   Loader2,
//   Cpu,
//   Cloud,
//   Zap,
//   RefreshCw,
// } from "lucide-react";
// import api from "@/lib/api";

// // ── Types ─────────────────────────────────────────────────────────────────────

// interface Credential {
//   credentialId: string;
//   credentialLabel: string;
//   provider: "AWS" | "AZURE";
// }

// interface DeploymentResult {
//   planId: string;
//   status: string;
//   userPrompt: string;
//   costEstimate: number | null;
//   aiGeneratedConfig: Record<string, unknown>;
//   credentialId: string;
//   createdAt: string;
//   updatedAt: string;
// }

// type PageState = "idle" | "loading" | "success" | "error";

// // ── Syntax Highlighting ───────────────────────────────────────────────────────

// function highlightJson(json: string): string {
//   if (!json) return "";
//   return json
//     .replace(/&/g, "&amp;")
//     .replace(/</g, "&lt;")
//     .replace(/>/g, "&gt;")
//     .replace(
//       /("(\\u[a-zA-Z0-9]{4}|\\[^u]|[^\\"])*"(\s*:)?|\b(true|false|null)\b|-?\d+(?:\.\d*)?(?:[eE][+-]?\d+)?)/g,
//       (match) => {
//         let cls = "text-amber-300";
//         if (/^"/.test(match)) {
//           cls = /:$/.test(match) ? "text-sky-300" : "text-emerald-300";
//         } else if (/true|false/.test(match)) {
//           cls = "text-violet-400";
//         } else if (/null/.test(match)) {
//           cls = "text-rose-400";
//         }
//         return `<span class="${cls}">${match}</span>`;
//       }
//     );
// }

// // ── Provider Badge ────────────────────────────────────────────────────────────

// function ProviderBadge({ provider }: { provider: "AWS" | "AZURE" }) {
//   const isAws = provider === "AWS";
//   return (
//     <span
//       className={`
//         inline-flex items-center gap-1 px-1.5 py-0.5 rounded text-[10px] font-bold
//         tracking-widest uppercase border
//         ${
//           isAws
//             ? "bg-orange-500/10 text-orange-400 border-orange-500/30"
//             : "bg-sky-500/10 text-sky-400 border-sky-500/30"
//         }
//       `}
//     >
//       {isAws ? "AWS" : "AZ"}
//     </span>
//   );
// }

// // ── Custom Select ─────────────────────────────────────────────────────────────

// function CredentialSelect({
//   credentials,
//   selected,
//   onSelect,
//   disabled,
// }: {
//   credentials: Credential[];
//   selected: string;
//   onSelect: (id: string) => void;
//   disabled: boolean;
// }) {
//   const [open, setOpen] = useState(false);
//   const ref = useRef<HTMLDivElement>(null);

// //   const selectedCred = credentials.find((c) => c.credentialId === selected);
// const selectedCred = credentials.find((c) => c.credentialId === selected);

//   useEffect(() => {
//     function handleClickOutside(e: MouseEvent) {
//       if (ref.current && !ref.current.contains(e.target as Node)) {
//         setOpen(false);
//       }
//     }
//     document.addEventListener("mousedown", handleClickOutside);
//     return () => document.removeEventListener("mousedown", handleClickOutside);
//   }, []);

//   return (
//     <div ref={ref} className="relative">
//       <button
//         type="button"
//         disabled={disabled}
//         onClick={() => setOpen((prev) => !prev)}
//         className={`
//           w-full flex items-center justify-between gap-3
//           px-4 py-3 rounded-xl border text-left text-sm
//           bg-slate-900/60 backdrop-blur-sm
//           border-slate-700/60 text-slate-200
//           transition-all duration-200
//           focus:outline-none focus:ring-1 focus:ring-sky-500/60 focus:border-sky-500/40
//           hover:border-slate-600/80 hover:bg-slate-900/80
//           disabled:opacity-50 disabled:cursor-not-allowed
//           ${open ? "border-sky-500/40 ring-1 ring-sky-500/30" : ""}
//         `}
//       >
//         <div className="flex items-center gap-3 min-w-0">
//           <div
//             className={`
//             w-7 h-7 rounded-lg flex items-center justify-center flex-shrink-0
//             ${selectedCred ? "bg-sky-500/15 border border-sky-500/30" : "bg-slate-800 border border-slate-700"}
//           `}
//           >
//             <Cloud
//               className={`w-3.5 h-3.5 ${selectedCred ? "text-sky-400" : "text-slate-600"}`}
//             />
//           </div>
//           {selectedCred ? (
//             <div className="flex items-center gap-2 min-w-0">
//               <span className="text-slate-200 font-medium truncate">
//                 {selectedCred.credentialLabel}
//               </span>
//               <ProviderBadge provider={selectedCred.provider} />
//             </div>
//           ) : (
//             <span className="text-slate-500 font-mono text-xs">
//               SELECT CREDENTIAL
//             </span>
//           )}
//         </div>
//         <ChevronDown
//           className={`w-4 h-4 text-slate-500 flex-shrink-0 transition-transform duration-200
//             ${open ? "rotate-180 text-sky-400" : ""}`}
//         />
//       </button>

//       {open && credentials.length > 0 && (
//         <div
//           className="
//             absolute z-50 w-full mt-2 py-1.5 rounded-xl border
//             bg-slate-900/95 backdrop-blur-xl border-slate-700/60
//             shadow-2xl shadow-black/60
//           "
//         >
//           {/* {credentials.map((cred) => (
//             <button
//               key={cred.publicId}
//               type="button"
//               onClick={() => {
//                 onSelect(cred.publicId);
//                 setOpen(false);
//               }} */}
//               {credentials.map((cred, index) => (
//                 <button
//                     // Fallback to 'id' if 'publicId' is missing, or use the array index as a last resort to prevent crashes
//                     key={cred.credentialId || (cred as any).id || `fallback-key-${index}`}
//               className={`
//                 w-full flex items-center gap-3 px-4 py-2.5 text-sm text-left
//                 transition-colors duration-150
//                 hover:bg-sky-500/10
//                 ${selected === cred.credentialId ? "bg-sky-500/10 text-sky-300" : "text-slate-300"}
//               `}
//             >
//               <div className="w-6 h-6 rounded-md bg-slate-800 border border-slate-700 flex items-center justify-center flex-shrink-0">
//                 <Cloud className="w-3 h-3 text-slate-500" />
//               </div>
//               <span className="font-medium truncate">{cred.credentialLabel}</span>
//               <ProviderBadge provider={cred.provider} />
//               {selected === cred.credentialId && (
//                 <CheckCircle2 className="w-3.5 h-3.5 ml-auto text-sky-400 flex-shrink-0" />
//               )}
//             </button>
//           ))}
//         </div>
//       )}

//       {open && credentials.length === 0 && (
//         <div className="absolute z-50 w-full mt-2 py-4 rounded-xl border bg-slate-900/95 border-slate-700/60 shadow-2xl text-center text-slate-500 text-sm">
//           No credentials found
//         </div>
//       )}
//     </div>
//   );
// }

// // ── AI Processing Loader ──────────────────────────────────────────────────────

// function AiProcessingLoader() {
//   const steps = [
//     "Parsing natural language prompt...",
//     "Consulting Gemini AI engine...",
//     "Translating to cloud SDK parameters...",
//     "Validating FinOps guardrails...",
//     "Generating deployment blueprint...",
//   ];
//   const [stepIndex, setStepIndex] = useState(0);

//   useEffect(() => {
//     const interval = setInterval(() => {
//       setStepIndex((prev) => (prev + 1) % steps.length);
//     }, 1800);
//     return () => clearInterval(interval);
//   }, []);

//   return (
//     <div className="rounded-2xl border border-sky-500/20 bg-sky-500/5 backdrop-blur-sm p-8 custom-fade-in">
//       <div className="flex flex-col items-center gap-6">
//         <div className="relative">
//           <div className="absolute inset-0 rounded-full bg-sky-500/20 animate-ping" />
//           <div className="relative w-14 h-14 rounded-full bg-slate-900 border border-sky-500/40 flex items-center justify-center">
//             <Cpu className="w-6 h-6 text-sky-400 animate-pulse" />
//           </div>
//         </div>

//         <div className="text-center">
//           <p className="text-sky-300 font-semibold tracking-wide text-sm uppercase">
//             AI Processing
//           </p>
//           <p className="text-slate-500 text-xs mt-1 font-mono">
//             gemini-pro engaged
//           </p>
//         </div>

//         <div className="w-full max-w-sm h-6 overflow-hidden relative">
//           <p key={stepIndex} className="text-slate-400 text-xs text-center font-mono custom-fade-in">
//             {steps[stepIndex]}
//           </p>
//         </div>

//         <div className="w-full max-w-sm h-px bg-slate-800 rounded-full overflow-hidden">
//           <div className="h-full bg-gradient-to-r from-sky-500 via-violet-500 to-sky-500 bg-[length:200%_100%] custom-shimmer rounded-full" />
//         </div>

//         <div className="w-full space-y-2">
//           {[80, 60, 90, 45].map((width, i) => (
//             <div
//               key={i}
//               className="h-2 rounded-full bg-slate-800/80 animate-pulse"
//               style={{ width: `${width}%`, animationDelay: `${i * 150}ms` }}
//             />
//           ))}
//         </div>
//       </div>
//     </div>
//   );
// }

// // ── Result Panel ──────────────────────────────────────────────────────────────

// function ResultPanel({ result }: { result: DeploymentResult }) {
//   const [copied, setCopied] = useState(false);
//   const formattedJson = JSON.stringify(result, null, 2);
//   const highlighted = highlightJson(formattedJson);

//   function handleCopy() {
//     navigator.clipboard.writeText(formattedJson).then(() => {
//       setCopied(true);
//       setTimeout(() => setCopied(false), 2000);
//     });
//   }

//   return (
//     <div className="space-y-4 custom-fade-in">
//       <div className="flex items-start gap-4 p-4 rounded-xl border border-emerald-500/25 bg-emerald-500/8 backdrop-blur-sm">
//         <div className="w-8 h-8 rounded-lg bg-emerald-500/15 border border-emerald-500/30 flex items-center justify-center flex-shrink-0 mt-0.5">
//           <CheckCircle2 className="w-4 h-4 text-emerald-400" />
//         </div>
//         <div className="flex-1 min-w-0">
//           <p className="text-emerald-300 font-semibold text-sm">
//             Blueprint Generated
//           </p>
//           <p className="text-slate-400 text-xs mt-0.5 font-mono">
//             Plan ID: {result.planId} &nbsp;·&nbsp; Status:{" "}
//             <span className="text-amber-400">{result.status}</span>
//             {result.costEstimate !== null && (
//               <>
//                 &nbsp;·&nbsp; Est. Cost:{" "}
//                 <span className="text-emerald-400">
//                   ${result.costEstimate}/mo
//                 </span>
//               </>
//             )}
//           </p>
//         </div>
//       </div>

//       <div className="rounded-xl border border-slate-700/60 overflow-hidden">
//         <div className="flex items-center justify-between px-4 py-2.5 bg-slate-900/80 border-b border-slate-700/60">
//           <div className="flex items-center gap-2">
//             <div className="flex gap-1.5">
//               <div className="w-2.5 h-2.5 rounded-full bg-rose-500/60" />
//               <div className="w-2.5 h-2.5 rounded-full bg-amber-500/60" />
//               <div className="w-2.5 h-2.5 rounded-full bg-emerald-500/60" />
//             </div>
//             <span className="text-slate-500 text-xs font-mono ml-2">
//               deployment_blueprint.json
//             </span>
//           </div>
//           <button
//             onClick={handleCopy}
//             className="flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-mono
//               text-slate-400 hover:text-slate-200 hover:bg-slate-700/60
//               border border-transparent hover:border-slate-600/60
//               transition-all duration-150"
//           >
//             {copied ? (
//               <>
//                 <CheckCircle2 className="w-3 h-3 text-emerald-400" />
//                 <span className="text-emerald-400">Copied</span>
//               </>
//             ) : (
//               <>
//                 <svg
//                   className="w-3 h-3"
//                   viewBox="0 0 24 24"
//                   fill="none"
//                   stroke="currentColor"
//                   strokeWidth={2}
//                 >
//                   <rect x="9" y="9" width="13" height="13" rx="2" ry="2" />
//                   <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1" />
//                 </svg>
//                 Copy JSON
//               </>
//             )}
//           </button>
//         </div>
//         <pre className="p-5 overflow-x-auto text-xs leading-relaxed bg-slate-950/80 max-h-[520px] overflow-y-auto">
//           <code className="font-mono" dangerouslySetInnerHTML={{ __html: highlighted }} />
//         </pre>
//       </div>
//     </div>
//   );
// }

// // ── Result Panel ──────────────────────────────────────────────────────────────

// function ResultPanel({ 
//   result, 
//   onExecute, 
//   isExecuting, 
//   executeMessage 
// }: { 
//   result: DeploymentResult;
//   onExecute: (planId: string) => void;
//   isExecuting: boolean;
//   executeMessage: string | null;
// }) {
//   const [copied, setCopied] = useState(false);
//   const formattedJson = JSON.stringify(result, null, 2);
//   const highlighted = highlightJson(formattedJson);

//   function handleCopy() {
//     navigator.clipboard.writeText(formattedJson).then(() => {
//       setCopied(true);
//       setTimeout(() => setCopied(false), 2000);
//     });
//   }

//   return (
//     <div className="space-y-4 custom-fade-in">
//       <div className="flex items-start gap-4 p-4 rounded-xl border border-emerald-500/25 bg-emerald-500/8 backdrop-blur-sm">
//         <div className="w-8 h-8 rounded-lg bg-emerald-500/15 border border-emerald-500/30 flex items-center justify-center flex-shrink-0 mt-0.5">
//           <CheckCircle2 className="w-4 h-4 text-emerald-400" />
//         </div>
//         <div className="flex-1 min-w-0">
//           <p className="text-emerald-300 font-semibold text-sm">
//             Blueprint Generated
//           </p>
//           <p className="text-slate-400 text-xs mt-0.5 font-mono">
//             Plan ID: {result.planId} &nbsp;·&nbsp; Status:{" "}
//             <span className={result.status === 'PENDING' ? "text-amber-400" : "text-emerald-400"}>
//               {executeMessage ? "ACCEPTED" : result.status}
//             </span>
//             {result.costEstimate !== null && (
//               <>
//                 &nbsp;·&nbsp; Est. Cost:{" "}
//                 <span className="text-emerald-400">
//                   ${result.costEstimate}/mo
//                 </span>
//               </>
//             )}
//           </p>
//         </div>
//       </div>

//       <div className="rounded-xl border border-slate-700/60 overflow-hidden">
//         <div className="flex items-center justify-between px-4 py-2.5 bg-slate-900/80 border-b border-slate-700/60">
//           <div className="flex items-center gap-2">
//             <div className="flex gap-1.5">
//               <div className="w-2.5 h-2.5 rounded-full bg-rose-500/60" />
//               <div className="w-2.5 h-2.5 rounded-full bg-amber-500/60" />
//               <div className="w-2.5 h-2.5 rounded-full bg-emerald-500/60" />
//             </div>
//             <span className="text-slate-500 text-xs font-mono ml-2">
//               deployment_blueprint.json
//             </span>
//           </div>
//           <button
//             onClick={handleCopy}
//             className="flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-mono
//               text-slate-400 hover:text-slate-200 hover:bg-slate-700/60
//               border border-transparent hover:border-slate-600/60
//               transition-all duration-150"
//           >
//             {copied ? (
//               <>
//                 <CheckCircle2 className="w-3 h-3 text-emerald-400" />
//                 <span className="text-emerald-400">Copied</span>
//               </>
//             ) : (
//               <>
//                 <svg className="w-3 h-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}>
//                   <rect x="9" y="9" width="13" height="13" rx="2" ry="2" />
//                   <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1" />
//                 </svg>
//                 Copy JSON
//               </>
//             )}
//           </button>
//         </div>
//         <pre className="p-5 overflow-x-auto text-xs leading-relaxed bg-slate-950/80 max-h-[520px] overflow-y-auto scrollbar-thin scrollbar-track-slate-900 scrollbar-thumb-slate-700">
//           <code className="font-mono" dangerouslySetInnerHTML={{ __html: highlighted }} />
//         </pre>
//       </div>

//       {/* Execution Actions */}
//       {!executeMessage ? (
//         <div className="flex justify-end pt-2">
//           <button
//             onClick={() => onExecute(result.planId)}
//             disabled={isExecuting}
//             className={`
//               flex items-center gap-2 px-6 py-2.5 rounded-lg text-sm font-semibold transition-all duration-200
//               ${isExecuting 
//                 ? 'bg-emerald-500/50 text-emerald-200 cursor-not-allowed' 
//                 : 'bg-emerald-600 hover:bg-emerald-500 text-white shadow-lg shadow-emerald-900/50'}
//             `}
//           >
//             {isExecuting ? (
//               <>
//                 <Loader2 className="w-4 h-4 animate-spin" />
//                 Initiating Deployment...
//               </>
//             ) : (
//               <>
//                 <Zap className="w-4 h-4" />
//                 Confirm & Provision Infrastructure
//               </>
//             )}
//           </button>
//         </div>
//       ) : (
//         <div className="p-4 rounded-xl border border-sky-500/30 bg-sky-500/10 backdrop-blur-sm animate-fade-in flex gap-3">
//            <Zap className="w-5 h-5 text-sky-400 flex-shrink-0" />
//            <div>
//              <p className="text-sky-300 font-medium text-sm">Deployment Initiated</p>
//              <p className="text-slate-400 text-xs font-mono mt-1">{executeMessage}</p>
//            </div>
//         </div>
//       )}
//     </div>
//   );
// }

// // ── Error Panel ───────────────────────────────────────────────────────────────

// function ErrorPanel({ message, onRetry }: { message: string; onRetry: () => void; }) {
//   return (
//     <div className="flex items-start gap-4 p-4 rounded-xl border border-rose-500/25 bg-rose-500/8 custom-fade-in">
//       <div className="w-8 h-8 rounded-lg bg-rose-500/15 border border-rose-500/30 flex items-center justify-center flex-shrink-0 mt-0.5">
//         <AlertCircle className="w-4 h-4 text-rose-400" />
//       </div>
//       <div className="flex-1 min-w-0">
//         <p className="text-rose-300 font-semibold text-sm">Execution Failed</p>
//         <p className="text-slate-400 text-xs mt-0.5 font-mono break-words">
//           {message}
//         </p>
//       </div>
//       <button
//         onClick={onRetry}
//         className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs
//           text-slate-300 hover:text-white bg-slate-800 hover:bg-slate-700
//           border border-slate-700/60 hover:border-slate-600
//           transition-all duration-150 flex-shrink-0"
//       >
//         <RefreshCw className="w-3 h-3" />
//         Retry
//       </button>
//     </div>
//   );
// }

// // ── Main Page ─────────────────────────────────────────────────────────────────

// export default function OrchestratorPage() {
//   const [credentials, setCredentials] = useState<Credential[]>([]);
//   const [selectedCredentialId, setSelectedCredentialId] = useState<string>("");
//   const [prompt, setPrompt] = useState<string>("");
//   const [pageState, setPageState] = useState<PageState>("idle");
//   const [result, setResult] = useState<DeploymentResult | null>(null);
//   const [errorMessage, setErrorMessage] = useState<string>("");
//   const [credentialsLoading, setCredentialsLoading] = useState<boolean>(true);
//   const textareaRef = useRef<HTMLTextAreaElement>(null);

//   const [isExecuting, setIsExecuting] = useState<boolean>(false);
//   const [executeMessage, setExecuteMessage] = useState<string | null>(null);
//   const isFormValid = selectedCredentialId !== "" && prompt.trim().length >= 10;
//   const isSubmitting = pageState === "loading";

//   useEffect(() => {
//     async function fetchCredentials() {
//       setCredentialsLoading(true);
//       try {
//         const response = await api.get<{ credentials: Credential[] }>("/credentials");
//         const list = response.data.credentials ?? [];
//         setCredentials(list);
//         if (list.length === 1) {
//           setSelectedCredentialId(list[0].credentialId);
//         }
//       } catch (err) {
//         console.error("[Orchestrator] Failed to load credentials:", err);
//         setCredentials([]);
//       } finally {
//         setCredentialsLoading(false);
//       }
//     }
//     fetchCredentials();
//   }, []);

//   useEffect(() => {
//     const textarea = textareaRef.current;
//     if (!textarea) return;
//     textarea.style.height = "auto";
//     textarea.style.height = `${Math.max(textarea.scrollHeight, 140)}px`;
//   }, [prompt]);

//   async function handleDeploy() {
//     if (!isFormValid || isSubmitting) return;

//     setPageState("loading");
//     setResult(null);
//     setErrorMessage("");

//     try {
//       // NOTE: Pointing directly to your backend Orchestrator endpoint.
//     //   const response = await api.post<DeploymentResult>("/orchestrator/deploy", {
//       const response = await api.post<DeploymentResult>("/deployments/generate", {
//         prompt: prompt.trim(),
//         credentialId: selectedCredentialId,
//       });
//       setResult(response.data);
//       setPageState("success");
//     } catch (err: unknown) {
//       const message =
//         (err as { response?: { data?: { message?: string } }; message?: string })
//           ?.response?.data?.message ??
//         (err as { message?: string })?.message ??
//         "An unexpected error occurred. Please try again.";
//       setErrorMessage(message);
//       setPageState("error");
//     }
//   }
//   async function handleExecutePlan(planId: string) {
//     setIsExecuting(true);
//     setExecuteMessage(null);
//     try {
//       // Calls your POST /{planId}/execute endpoint
//       const response = await api.post(`/deployments/${planId}/execute`);
//     //   setExecuteMessage(response.data.message || "Deployment execution has been triggered.");
//     setExecuteMessage("Provisioning workflow started. Systems are allocating resources in the background.");
//     } catch (err: unknown) {
//       const message = (err as any)?.response?.data?.message || "Failed to trigger deployment.";
//       setErrorMessage(message);
//       setPageState("error");
//     } finally {
//       setIsExecuting(false);
//     }
//   }
//   function handleReset() {
//     setPageState("idle");
//     setResult(null);
//     setErrorMessage("");
//   }

//   function handleKeyDown(e: React.KeyboardEvent<HTMLTextAreaElement>) {
//     if ((e.metaKey || e.ctrlKey) && e.key === "Enter") {
//       e.preventDefault();
//       handleDeploy();
//     }
//   }

// //   const selectedCred = credentials.find((c) => c.credentialId === selectedCredentialId);
// const selectedCred = credentials.find((c) => c.credentialId === selectedCredentialId);

//   return (
//     <div className="min-h-screen bg-slate-950 text-slate-100">
//       {/* Compiler-Safe Custom Styles */}
//       <style dangerouslySetInnerHTML={{
//         __html: `
//           @keyframes fadeIn { from { opacity: 0; transform: translateY(6px); } to { opacity: 1; transform: translateY(0); } }
//           .custom-fade-in { animation: fadeIn 0.3s ease-out forwards; }
//           @keyframes customShimmer { 0% { background-position: 200% center; } 100% { background-position: -200% center; } }
//           .custom-shimmer { animation: customShimmer 2s linear infinite; }
//         `
//       }} />

//       <div
//         className="fixed inset-0 opacity-[0.015] pointer-events-none"
//         style={{
//           backgroundImage:
//             "linear-gradient(rgba(148,163,184,1) 1px, transparent 1px), linear-gradient(90deg, rgba(148,163,184,1) 1px, transparent 1px)",
//           backgroundSize: "48px 48px",
//         }}
//       />
//       <div className="fixed top-0 left-1/2 -translate-x-1/2 w-[800px] h-[400px] bg-sky-500/4 rounded-full blur-3xl pointer-events-none" />

//       <div className="relative max-w-4xl mx-auto px-6 py-10 space-y-8">
//         <div className="flex items-start justify-between">
//           <div className="space-y-1">
//             <div className="flex items-center gap-3">
//               <div className="w-9 h-9 rounded-xl bg-sky-500/15 border border-sky-500/30 flex items-center justify-center">
//                 <Zap className="w-4.5 h-4.5 text-sky-400" />
//               </div>
//               <h1 className="text-2xl font-bold tracking-tight text-white">
//                 AI Orchestrator
//               </h1>
//             </div>
//             <p className="text-slate-500 text-sm pl-12">
//               Natural language to infrastructure.
//             </p>
//           </div>

//           <div className="flex items-center gap-2 px-3 py-1.5 rounded-full border border-slate-700/60 bg-slate-900/60 backdrop-blur-sm">
//             <div
//               className={`w-1.5 h-1.5 rounded-full ${
//                 isSubmitting
//                   ? "bg-amber-400 animate-pulse"
//                   : pageState === "success"
//                     ? "bg-emerald-400"
//                     : pageState === "error"
//                       ? "bg-rose-400"
//                       : "bg-slate-600"
//               }`}
//             />
//             <span className="text-xs font-mono text-slate-400 uppercase tracking-widest">
//               {isSubmitting
//                 ? "executing"
//                 : pageState === "success"
//                   ? "deployed"
//                   : pageState === "error"
//                     ? "failed"
//                     : "standby"}
//             </span>
//           </div>
//         </div>

//         <div className="rounded-2xl border border-slate-700/50 bg-slate-900/40 backdrop-blur-sm p-6 space-y-5">
//           <div className="flex items-center gap-2 mb-1">
//             <Cloud className="w-3.5 h-3.5 text-slate-500" />
//             <span className="text-xs font-mono text-slate-500 uppercase tracking-widest">
//               Target Credential
//             </span>
//           </div>

//           {credentialsLoading ? (
//             <div className="h-12 rounded-xl bg-slate-800/60 border border-slate-700/40 animate-pulse" />
//           ) : (
//             <CredentialSelect
//               credentials={credentials}
//               selected={selectedCredentialId}
//               onSelect={setSelectedCredentialId}
//               disabled={isSubmitting}
//             />
//           )}

//           {credentials.length === 0 && !credentialsLoading && (
//             <p className="text-xs text-amber-400/80 font-mono flex items-center gap-2">
//               <AlertCircle className="w-3.5 h-3.5" />
//               No credentials configured. Add cloud credentials in the Vault
//               first.
//             </p>
//           )}

//           {selectedCred && (
//             <div className="flex items-center gap-2 text-xs font-mono text-slate-600">
//               <div className="w-1 h-1 rounded-full bg-slate-700" />
//               <span>ID: {selectedCred.credentialId}</span>
//               <div className="w-1 h-1 rounded-full bg-slate-700" />
//               <span className="uppercase">{selectedCred.provider}</span>
//             </div>
//           )}
//         </div>

//         <div className="rounded-2xl border border-slate-700/50 bg-slate-900/40 backdrop-blur-sm overflow-hidden">
//           <div className="flex items-center justify-between px-5 py-3 border-b border-slate-700/50 bg-slate-900/60">
//             <div className="flex items-center gap-3">
//               <div className="flex gap-1.5">
//                 <div className="w-2.5 h-2.5 rounded-full bg-rose-500/50" />
//                 <div className="w-2.5 h-2.5 rounded-full bg-amber-500/50" />
//                 <div className="w-2.5 h-2.5 rounded-full bg-emerald-500/50" />
//               </div>
//               <div className="flex items-center gap-2">
//                 <Terminal className="w-3.5 h-3.5 text-slate-500" />
//                 <span className="text-xs font-mono text-slate-500">
//                   mindops@orchestrator:~$
//                 </span>
//               </div>
//             </div>
//             <span className="text-xs font-mono text-slate-600">
//               ⌘ + Enter to deploy
//             </span>
//           </div>

//           <div className="relative">
//             <div className="absolute top-4 left-5 text-slate-600 font-mono text-sm select-none pointer-events-none">
//               &gt;
//             </div>
//             <textarea
//               ref={textareaRef}
//               value={prompt}
//               onChange={(e) => setPrompt(e.target.value)}
//               onKeyDown={handleKeyDown}
//               disabled={isSubmitting}
//               placeholder="E.g., Deploy a highly available Ubuntu EC2 instance in us-east-1 with auto-scaling between 2 and 10 nodes..."
//               className="
//                 w-full pl-10 pr-5 py-4 min-h-[140px]
//                 bg-transparent resize-none
//                 font-mono text-sm text-slate-200 leading-relaxed
//                 placeholder:text-slate-700
//                 focus:outline-none
//                 disabled:opacity-50 disabled:cursor-not-allowed
//                 transition-colors duration-200
//               "
//               style={{ height: "auto" }}
//             />
//           </div>

//           <div className="flex items-center justify-between px-5 py-3 border-t border-slate-800/60 bg-slate-950/30">
//             <div className="flex items-center gap-3">
//               <span
//                 className={`text-xs font-mono ${
//                   prompt.length > 1800
//                     ? "text-rose-400"
//                     : prompt.length > 1200
//                       ? "text-amber-400"
//                       : "text-slate-600"
//                 }`}
//               >
//                 {prompt.length} / 2000
//               </span>
//               {prompt.trim().length > 0 && prompt.trim().length < 10 && (
//                 <span className="text-xs font-mono text-amber-500/70">
//                   prompt too short
//                 </span>
//               )}
//             </div>

//             <button
//               onClick={handleDeploy}
//               disabled={!isFormValid || isSubmitting}
//               className={`
//                 flex items-center gap-2.5 px-5 py-2.5 rounded-xl
//                 text-sm font-semibold
//                 transition-all duration-200
//                 focus:outline-none focus:ring-2 focus:ring-sky-500/40
//                 ${
//                   isFormValid && !isSubmitting
//                     ? `bg-sky-500 hover:bg-sky-400 text-white
//                        shadow-lg shadow-sky-500/25 hover:shadow-sky-400/35
//                        active:scale-95`
//                     : "bg-slate-800 text-slate-600 cursor-not-allowed border border-slate-700/60"
//                 }
//               `}
//             >
//               {isSubmitting ? (
//                 <>
//                   <Loader2 className="w-4 h-4 animate-spin" />
//                   <span>Processing...</span>
//                 </>
//               ) : (
//                 <>
//                   <Terminal className="w-4 h-4" />
//                   <span>Deploy Infrastructure</span>
//                 </>
//               )}
//             </button>
//           </div>
//         </div>

//         {pageState !== "idle" && (
//           <div className="space-y-2">
//             <div className="flex items-center justify-between">
//               <div className="flex items-center gap-2">
//                 <div className="w-1 h-4 rounded-full bg-sky-500/60" />
//                 <span className="text-xs font-mono text-slate-500 uppercase tracking-widest">
//                   Execution Output
//                 </span>
//               </div>
//               {(pageState === "success" || pageState === "error") && (
//                 <button
//                   onClick={handleReset}
//                   className="flex items-center gap-1.5 text-xs font-mono text-slate-600
//                     hover:text-slate-400 transition-colors duration-150"
//                 >
//                   <RefreshCw className="w-3 h-3" />
//                   New deployment
//                 </button>
//               )}
//             </div>

//             {pageState === "loading" && <AiProcessingLoader />}
//             {/* {pageState === "success" && result && <ResultPanel result={result} />} */}
//             {pageState === "success" && result && (
//                 <ResultPanel 
//                 result={result} 
//                 onExecute={handleExecutePlan}
//                 isExecuting={isExecuting}
//                 executeMessage={executeMessage}
//                 />
//             )}
//             {pageState === "error" && <ErrorPanel message={errorMessage} onRetry={handleReset} />}
//           </div>
//         )}

//         <div className="flex items-center justify-between pt-2 border-t border-slate-800/60">
//           <p className="text-xs font-mono text-slate-700">
//             mindops-cloud · orchestrator v1.0
//           </p>
//           <p className="text-xs font-mono text-slate-700">
//             powered by Gemini AI
//           </p>
//         </div>
//       </div>
//     </div>
//   );
// }
"use client";

import { useEffect, useState, useRef } from "react";
import {
  Terminal,
  ChevronDown,
  CheckCircle2,
  AlertCircle,
  Loader2,
  Cpu,
  Cloud,
  Zap,
  RefreshCw,
} from "lucide-react";
import api from "@/lib/api";

// ── Types ─────────────────────────────────────────────────────────────────────

interface Credential {
  credentialId: string;
  credentialLabel: string;
  provider: "AWS" | "AZURE";
}

interface DeploymentResult {
  planId: string;
  status: string;
  userPrompt: string;
  costEstimate: number | null;
  aiGeneratedConfig: Record<string, unknown>;
  credentialId: string;
  createdAt: string;
  updatedAt: string;
}

type PageState = "idle" | "loading" | "success" | "error";

// ── Syntax Highlighting ───────────────────────────────────────────────────────

function highlightJson(json: string): string {
  if (!json) return "";
  return json
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(
      /("(\\u[a-zA-Z0-9]{4}|\\[^u]|[^\\"])*"(\s*:)?|\b(true|false|null)\b|-?\d+(?:\.\d*)?(?:[eE][+-]?\d+)?)/g,
      (match) => {
        let cls = "text-amber-300";
        if (/^"/.test(match)) {
          cls = /:$/.test(match) ? "text-sky-300" : "text-emerald-300";
        } else if (/true|false/.test(match)) {
          cls = "text-violet-400";
        } else if (/null/.test(match)) {
          cls = "text-rose-400";
        }
        return `<span class="${cls}">${match}</span>`;
      }
    );
}

// ── Provider Badge ────────────────────────────────────────────────────────────

function ProviderBadge({ provider }: { provider: "AWS" | "AZURE" }) {
  const isAws = provider === "AWS";
  return (
    <span
      className={`
        inline-flex items-center gap-1 px-1.5 py-0.5 rounded text-[10px] font-bold
        tracking-widest uppercase border
        ${
          isAws
            ? "bg-orange-500/10 text-orange-400 border-orange-500/30"
            : "bg-sky-500/10 text-sky-400 border-sky-500/30"
        }
      `}
    >
      {isAws ? "AWS" : "AZ"}
    </span>
  );
}

// ── Custom Select ─────────────────────────────────────────────────────────────

function CredentialSelect({
  credentials,
  selected,
  onSelect,
  disabled,
}: {
  credentials: Credential[];
  selected: string;
  onSelect: (id: string) => void;
  disabled: boolean;
}) {
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  const selectedCred = credentials.find((c) => c.credentialId === selected);

  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  return (
    <div ref={ref} className="relative">
      <button
        type="button"
        disabled={disabled}
        onClick={() => setOpen((prev) => !prev)}
        className={`
          w-full flex items-center justify-between gap-3
          px-4 py-3 rounded-xl border text-left text-sm
          bg-slate-900/60 backdrop-blur-sm
          border-slate-700/60 text-slate-200
          transition-all duration-200
          focus:outline-none focus:ring-1 focus:ring-sky-500/60 focus:border-sky-500/40
          hover:border-slate-600/80 hover:bg-slate-900/80
          disabled:opacity-50 disabled:cursor-not-allowed
          ${open ? "border-sky-500/40 ring-1 ring-sky-500/30" : ""}
        `}
      >
        <div className="flex items-center gap-3 min-w-0">
          <div
            className={`
            w-7 h-7 rounded-lg flex items-center justify-center flex-shrink-0
            ${selectedCred ? "bg-sky-500/15 border border-sky-500/30" : "bg-slate-800 border border-slate-700"}
          `}
          >
            <Cloud
              className={`w-3.5 h-3.5 ${selectedCred ? "text-sky-400" : "text-slate-600"}`}
            />
          </div>
          {selectedCred ? (
            <div className="flex items-center gap-2 min-w-0">
              <span className="text-slate-200 font-medium truncate">
                {selectedCred.credentialLabel}
              </span>
              <ProviderBadge provider={selectedCred.provider} />
            </div>
          ) : (
            <span className="text-slate-500 font-mono text-xs">
              SELECT CREDENTIAL
            </span>
          )}
        </div>
        <ChevronDown
          className={`w-4 h-4 text-slate-500 flex-shrink-0 transition-transform duration-200
            ${open ? "rotate-180 text-sky-400" : ""}`}
        />
      </button>

      {open && credentials.length > 0 && (
        <div
          className="
            absolute z-50 w-full mt-2 py-1.5 rounded-xl border
            bg-slate-900/95 backdrop-blur-xl border-slate-700/60
            shadow-2xl shadow-black/60
          "
        >
          {credentials.map((cred, index) => (
            <button
              key={cred.credentialId || (cred as any).id || `fallback-key-${index}`}
              type="button"
              onClick={() => {
                onSelect(cred.credentialId);
                setOpen(false);
              }}
              className={`
                w-full flex items-center gap-3 px-4 py-2.5 text-sm text-left
                transition-colors duration-150
                hover:bg-sky-500/10
                ${selected === cred.credentialId ? "bg-sky-500/10 text-sky-300" : "text-slate-300"}
              `}
            >
              <div className="w-6 h-6 rounded-md bg-slate-800 border border-slate-700 flex items-center justify-center flex-shrink-0">
                <Cloud className="w-3 h-3 text-slate-500" />
              </div>
              <span className="font-medium truncate">{cred.credentialLabel}</span>
              <ProviderBadge provider={cred.provider} />
              {selected === cred.credentialId && (
                <CheckCircle2 className="w-3.5 h-3.5 ml-auto text-sky-400 flex-shrink-0" />
              )}
            </button>
          ))}
        </div>
      )}

      {open && credentials.length === 0 && (
        <div className="absolute z-50 w-full mt-2 py-4 rounded-xl border bg-slate-900/95 border-slate-700/60 shadow-2xl text-center text-slate-500 text-sm">
          No credentials found
        </div>
      )}
    </div>
  );
}

// ── AI Processing Loader ──────────────────────────────────────────────────────

function AiProcessingLoader() {
  const steps = [
    "Parsing natural language prompt...",
    "Consulting Gemini AI engine...",
    "Translating to cloud SDK parameters...",
    "Validating FinOps guardrails...",
    "Generating deployment blueprint...",
  ];
  const [stepIndex, setStepIndex] = useState(0);

  useEffect(() => {
    const interval = setInterval(() => {
      setStepIndex((prev) => (prev + 1) % steps.length);
    }, 1800);
    return () => clearInterval(interval);
  }, []);

  return (
    <div className="rounded-2xl border border-sky-500/20 bg-sky-500/5 backdrop-blur-sm p-8 custom-fade-in">
      <div className="flex flex-col items-center gap-6">
        <div className="relative">
          <div className="absolute inset-0 rounded-full bg-sky-500/20 animate-ping" />
          <div className="relative w-14 h-14 rounded-full bg-slate-900 border border-sky-500/40 flex items-center justify-center">
            <Cpu className="w-6 h-6 text-sky-400 animate-pulse" />
          </div>
        </div>

        <div className="text-center">
          <p className="text-sky-300 font-semibold tracking-wide text-sm uppercase">
            AI Processing
          </p>
          <p className="text-slate-500 text-xs mt-1 font-mono">
            gemini-pro engaged
          </p>
        </div>

        <div className="w-full max-w-sm h-6 overflow-hidden relative">
          <p key={stepIndex} className="text-slate-400 text-xs text-center font-mono custom-fade-in">
            {steps[stepIndex]}
          </p>
        </div>

        <div className="w-full max-w-sm h-px bg-slate-800 rounded-full overflow-hidden">
          <div className="h-full bg-gradient-to-r from-sky-500 via-violet-500 to-sky-500 bg-[length:200%_100%] custom-shimmer rounded-full" />
        </div>

        <div className="w-full space-y-2">
          {[80, 60, 90, 45].map((width, i) => (
            <div
              key={i}
              className="h-2 rounded-full bg-slate-800/80 animate-pulse"
              style={{ width: `${width}%`, animationDelay: `${i * 150}ms` }}
            />
          ))}
        </div>
      </div>
    </div>
  );
}

// ── Result Panel ──────────────────────────────────────────────────────────────

function ResultPanel({ 
  result, 
  onExecute, 
  isExecuting, 
  executeMessage 
}: { 
  result: DeploymentResult;
  onExecute: (planId: string) => void;
  isExecuting: boolean;
  executeMessage: string | null;
}) {
  const [copied, setCopied] = useState(false);
  const formattedJson = JSON.stringify(result, null, 2);
  const highlighted = highlightJson(formattedJson);

  function handleCopy() {
    navigator.clipboard.writeText(formattedJson).then(() => {
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    });
  }

  return (
    <div className="space-y-4 custom-fade-in">
      <div className="flex items-start gap-4 p-4 rounded-xl border border-emerald-500/25 bg-emerald-500/8 backdrop-blur-sm">
        <div className="w-8 h-8 rounded-lg bg-emerald-500/15 border border-emerald-500/30 flex items-center justify-center flex-shrink-0 mt-0.5">
          <CheckCircle2 className="w-4 h-4 text-emerald-400" />
        </div>
        <div className="flex-1 min-w-0">
          <p className="text-emerald-300 font-semibold text-sm">
            Blueprint Generated
          </p>
          <p className="text-slate-400 text-xs mt-0.5 font-mono">
            Plan ID: {result.planId} &nbsp;·&nbsp; Status:{" "}
            <span className={result.status === 'PENDING' ? "text-amber-400" : "text-emerald-400"}>
              {executeMessage ? "ACCEPTED" : result.status}
            </span>
            {result.costEstimate !== null && (
              <>
                &nbsp;·&nbsp; Est. Cost:{" "}
                <span className="text-emerald-400">
                  ${result.costEstimate}/mo
                </span>
              </>
            )}
          </p>
        </div>
      </div>

      <div className="rounded-xl border border-slate-700/60 overflow-hidden">
        <div className="flex items-center justify-between px-4 py-2.5 bg-slate-900/80 border-b border-slate-700/60">
          <div className="flex items-center gap-2">
            <div className="flex gap-1.5">
              <div className="w-2.5 h-2.5 rounded-full bg-rose-500/60" />
              <div className="w-2.5 h-2.5 rounded-full bg-amber-500/60" />
              <div className="w-2.5 h-2.5 rounded-full bg-emerald-500/60" />
            </div>
            <span className="text-slate-500 text-xs font-mono ml-2">
              deployment_blueprint.json
            </span>
          </div>
          <button
            onClick={handleCopy}
            className="flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-mono
              text-slate-400 hover:text-slate-200 hover:bg-slate-700/60
              border border-transparent hover:border-slate-600/60
              transition-all duration-150"
          >
            {copied ? (
              <>
                <CheckCircle2 className="w-3 h-3 text-emerald-400" />
                <span className="text-emerald-400">Copied</span>
              </>
            ) : (
              <>
                <svg className="w-3 h-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}>
                  <rect x="9" y="9" width="13" height="13" rx="2" ry="2" />
                  <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1" />
                </svg>
                Copy JSON
              </>
            )}
          </button>
        </div>
        <pre className="p-5 overflow-x-auto text-xs leading-relaxed bg-slate-950/80 max-h-[520px] overflow-y-auto scrollbar-thin scrollbar-track-slate-900 scrollbar-thumb-slate-700">
          <code className="font-mono" dangerouslySetInnerHTML={{ __html: highlighted }} />
        </pre>
      </div>

      {/* Execution Actions */}
      {!executeMessage ? (
        <div className="flex justify-end pt-2">
          <button
            onClick={() => onExecute(result.planId)}
            disabled={isExecuting}
            className={`
              flex items-center gap-2 px-6 py-2.5 rounded-lg text-sm font-semibold transition-all duration-200
              ${isExecuting 
                ? 'bg-emerald-500/50 text-emerald-200 cursor-not-allowed' 
                : 'bg-emerald-600 hover:bg-emerald-500 text-white shadow-lg shadow-emerald-900/50'}
            `}
          >
            {isExecuting ? (
              <>
                <Loader2 className="w-4 h-4 animate-spin" />
                Initiating Deployment...
              </>
            ) : (
              <>
                <Zap className="w-4 h-4" />
                Confirm & Provision Infrastructure
              </>
            )}
          </button>
        </div>
      ) : (
        <div className="p-4 rounded-xl border border-sky-500/30 bg-sky-500/10 backdrop-blur-sm animate-fade-in flex gap-3">
           <Zap className="w-5 h-5 text-sky-400 flex-shrink-0" />
           <div>
             <p className="text-sky-300 font-medium text-sm">Deployment Initiated</p>
             <p className="text-slate-400 text-xs font-mono mt-1">{executeMessage}</p>
           </div>
        </div>
      )}
    </div>
  );
}

// ── Error Panel ───────────────────────────────────────────────────────────────

function ErrorPanel({ message, onRetry }: { message: string; onRetry: () => void; }) {
  return (
    <div className="flex items-start gap-4 p-4 rounded-xl border border-rose-500/25 bg-rose-500/8 custom-fade-in">
      <div className="w-8 h-8 rounded-lg bg-rose-500/15 border border-rose-500/30 flex items-center justify-center flex-shrink-0 mt-0.5">
        <AlertCircle className="w-4 h-4 text-rose-400" />
      </div>
      <div className="flex-1 min-w-0">
        <p className="text-rose-300 font-semibold text-sm">Execution Failed</p>
        <p className="text-slate-400 text-xs mt-0.5 font-mono break-words">
          {message}
        </p>
      </div>
      <button
        onClick={onRetry}
        className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs
          text-slate-300 hover:text-white bg-slate-800 hover:bg-slate-700
          border border-slate-700/60 hover:border-slate-600
          transition-all duration-150 flex-shrink-0"
      >
        <RefreshCw className="w-3 h-3" />
        Retry
      </button>
    </div>
  );
}

// ── Main Page ─────────────────────────────────────────────────────────────────

export default function OrchestratorPage() {
  const [credentials, setCredentials] = useState<Credential[]>([]);
  const [selectedCredentialId, setSelectedCredentialId] = useState<string>("");
  const [prompt, setPrompt] = useState<string>("");
  const [pageState, setPageState] = useState<PageState>("idle");
  const [result, setResult] = useState<DeploymentResult | null>(null);
  const [errorMessage, setErrorMessage] = useState<string>("");
  const [credentialsLoading, setCredentialsLoading] = useState<boolean>(true);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  const [isExecuting, setIsExecuting] = useState<boolean>(false);
  const [executeMessage, setExecuteMessage] = useState<string | null>(null);
  const isFormValid = selectedCredentialId !== "" && prompt.trim().length >= 10;
  const isSubmitting = pageState === "loading";

  useEffect(() => {
    async function fetchCredentials() {
      setCredentialsLoading(true);
      try {
        const response = await api.get<{ credentials: Credential[] }>("/credentials");
        const list = response.data.credentials ?? [];
        setCredentials(list);
        if (list.length === 1) {
          setSelectedCredentialId(list[0].credentialId);
        }
      } catch (err) {
        console.error("[Orchestrator] Failed to load credentials:", err);
        setCredentials([]);
      } finally {
        setCredentialsLoading(false);
      }
    }
    fetchCredentials();
  }, []);

  useEffect(() => {
    const textarea = textareaRef.current;
    if (!textarea) return;
    textarea.style.height = "auto";
    textarea.style.height = `${Math.max(textarea.scrollHeight, 140)}px`;
  }, [prompt]);

  async function handleDeploy() {
    if (!isFormValid || isSubmitting) return;

    setPageState("loading");
    setResult(null);
    setErrorMessage("");

    try {
      const response = await api.post<DeploymentResult>("/deployments/generate", {
        prompt: prompt.trim(),
        credentialId: selectedCredentialId,
      });
      setResult(response.data);
      setPageState("success");
    } catch (err: unknown) {
      const message =
        (err as { response?: { data?: { message?: string } }; message?: string })
          ?.response?.data?.message ??
        (err as { message?: string })?.message ??
        "An unexpected error occurred. Please try again.";
      setErrorMessage(message);
      setPageState("error");
    }
  }

  async function handleExecutePlan(planId: string) {
    setIsExecuting(true);
    setExecuteMessage(null);
    try {
      await api.post(`/deployments/${planId}/execute`);
      setExecuteMessage("Provisioning workflow started. Systems are allocating resources in the background.");
    } catch (err: unknown) {
      const message = (err as any)?.response?.data?.message || "Failed to trigger deployment.";
      setErrorMessage(message);
      setPageState("error");
    } finally {
      setIsExecuting(false);
    }
  }

  function handleReset() {
    setPageState("idle");
    setResult(null);
    setErrorMessage("");
  }

  function handleKeyDown(e: React.KeyboardEvent<HTMLTextAreaElement>) {
    if ((e.metaKey || e.ctrlKey) && e.key === "Enter") {
      e.preventDefault();
      handleDeploy();
    }
  }

  const selectedCred = credentials.find((c) => c.credentialId === selectedCredentialId);

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100">
      {/* Compiler-Safe Custom Styles */}
      <style dangerouslySetInnerHTML={{
        __html: `
          @keyframes fadeIn { from { opacity: 0; transform: translateY(6px); } to { opacity: 1; transform: translateY(0); } }
          .custom-fade-in { animation: fadeIn 0.3s ease-out forwards; }
          @keyframes customShimmer { 0% { background-position: 200% center; } 100% { background-position: -200% center; } }
          .custom-shimmer { animation: customShimmer 2s linear infinite; }
        `
      }} />

      <div
        className="fixed inset-0 opacity-[0.015] pointer-events-none"
        style={{
          backgroundImage:
            "linear-gradient(rgba(148,163,184,1) 1px, transparent 1px), linear-gradient(90deg, rgba(148,163,184,1) 1px, transparent 1px)",
          backgroundSize: "48px 48px",
        }}
      />
      <div className="fixed top-0 left-1/2 -translate-x-1/2 w-[800px] h-[400px] bg-sky-500/4 rounded-full blur-3xl pointer-events-none" />

      <div className="relative max-w-4xl mx-auto px-6 py-10 space-y-8">
        <div className="flex items-start justify-between">
          <div className="space-y-1">
            <div className="flex items-center gap-3">
              <div className="w-9 h-9 rounded-xl bg-sky-500/15 border border-sky-500/30 flex items-center justify-center">
                <Zap className="w-4.5 h-4.5 text-sky-400" />
              </div>
              <h1 className="text-2xl font-bold tracking-tight text-white">
                AI Orchestrator
              </h1>
            </div>
            <p className="text-slate-500 text-sm pl-12">
              Natural language to infrastructure.
            </p>
          </div>

          <div className="flex items-center gap-2 px-3 py-1.5 rounded-full border border-slate-700/60 bg-slate-900/60 backdrop-blur-sm">
            <div
              className={`w-1.5 h-1.5 rounded-full ${
                isSubmitting
                  ? "bg-amber-400 animate-pulse"
                  : pageState === "success"
                    ? "bg-emerald-400"
                    : pageState === "error"
                      ? "bg-rose-400"
                      : "bg-slate-600"
              }`}
            />
            <span className="text-xs font-mono text-slate-400 uppercase tracking-widest">
              {isSubmitting
                ? "executing"
                : pageState === "success"
                  ? "deployed"
                  : pageState === "error"
                    ? "failed"
                    : "standby"}
            </span>
          </div>
        </div>

        {/* ── Z-INDEX FIX APPLIED HERE ─────────────────────────────────────── */}
        <div className="relative z-[100] rounded-2xl border border-slate-700/50 bg-slate-900/40 backdrop-blur-sm p-6 space-y-5">
          <div className="flex items-center gap-2 mb-1">
            <Cloud className="w-3.5 h-3.5 text-slate-500" />
            <span className="text-xs font-mono text-slate-500 uppercase tracking-widest">
              Target Credential
            </span>
          </div>

          {credentialsLoading ? (
            <div className="h-12 rounded-xl bg-slate-800/60 border border-slate-700/40 animate-pulse" />
          ) : (
            <CredentialSelect
              credentials={credentials}
              selected={selectedCredentialId}
              onSelect={setSelectedCredentialId}
              disabled={isSubmitting}
            />
          )}

          {credentials.length === 0 && !credentialsLoading && (
            <p className="text-xs text-amber-400/80 font-mono flex items-center gap-2">
              <AlertCircle className="w-3.5 h-3.5" />
              No credentials configured. Add cloud credentials in the Vault first.
            </p>
          )}

          {selectedCred && (
            <div className="flex items-center gap-2 text-xs font-mono text-slate-600">
              <div className="w-1 h-1 rounded-full bg-slate-700" />
              <span>ID: {selectedCred.credentialId}</span>
              <div className="w-1 h-1 rounded-full bg-slate-700" />
              <span className="uppercase">{selectedCred.provider}</span>
            </div>
          )}
        </div>

        {/* ── Z-INDEX FIX APPLIED HERE ─────────────────────────────────────── */}
        <div className="relative z-10 rounded-2xl border border-slate-700/50 bg-slate-900/40 backdrop-blur-sm overflow-hidden">
          <div className="flex items-center justify-between px-5 py-3 border-b border-slate-700/50 bg-slate-900/60">
            <div className="flex items-center gap-3">
              <div className="flex gap-1.5">
                <div className="w-2.5 h-2.5 rounded-full bg-rose-500/50" />
                <div className="w-2.5 h-2.5 rounded-full bg-amber-500/50" />
                <div className="w-2.5 h-2.5 rounded-full bg-emerald-500/50" />
              </div>
              <div className="flex items-center gap-2">
                <Terminal className="w-3.5 h-3.5 text-slate-500" />
                <span className="text-xs font-mono text-slate-500">
                  mindops@orchestrator:~$
                </span>
              </div>
            </div>
            <span className="text-xs font-mono text-slate-600">
              ⌘ + Enter to deploy
            </span>
          </div>

          <div className="relative">
            <div className="absolute top-4 left-5 text-slate-600 font-mono text-sm select-none pointer-events-none">
              &gt;
            </div>
            <textarea
              ref={textareaRef}
              value={prompt}
              onChange={(e) => setPrompt(e.target.value)}
              onKeyDown={handleKeyDown}
              disabled={isSubmitting}
              placeholder="E.g., Deploy a highly available Ubuntu EC2 instance in us-east-1 with auto-scaling between 2 and 10 nodes..."
              className="
                w-full pl-10 pr-5 py-4 min-h-[140px]
                bg-transparent resize-none
                font-mono text-sm text-slate-200 leading-relaxed
                placeholder:text-slate-700
                focus:outline-none
                disabled:opacity-50 disabled:cursor-not-allowed
                transition-colors duration-200
              "
              style={{ height: "auto" }}
            />
          </div>

          <div className="flex items-center justify-between px-5 py-3 border-t border-slate-800/60 bg-slate-950/30">
            <div className="flex items-center gap-3">
              <span
                className={`text-xs font-mono ${
                  prompt.length > 1800
                    ? "text-rose-400"
                    : prompt.length > 1200
                      ? "text-amber-400"
                      : "text-slate-600"
                }`}
              >
                {prompt.length} / 2000
              </span>
              {prompt.trim().length > 0 && prompt.trim().length < 10 && (
                <span className="text-xs font-mono text-amber-500/70">
                  prompt too short
                </span>
              )}
            </div>

            <button
              onClick={handleDeploy}
              disabled={!isFormValid || isSubmitting}
              className={`
                flex items-center gap-2.5 px-5 py-2.5 rounded-xl
                text-sm font-semibold
                transition-all duration-200
                focus:outline-none focus:ring-2 focus:ring-sky-500/40
                ${
                  isFormValid && !isSubmitting
                    ? `bg-sky-500 hover:bg-sky-400 text-white
                       shadow-lg shadow-sky-500/25 hover:shadow-sky-400/35
                       active:scale-95`
                    : "bg-slate-800 text-slate-600 cursor-not-allowed border border-slate-700/60"
                }
              `}
            >
              {isSubmitting ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" />
                  <span>Processing...</span>
                </>
              ) : (
                <>
                  <Terminal className="w-4 h-4" />
                  <span>Deploy Infrastructure</span>
                </>
              )}
            </button>
          </div>
        </div>

        {pageState !== "idle" && (
          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <div className="w-1 h-4 rounded-full bg-sky-500/60" />
                <span className="text-xs font-mono text-slate-500 uppercase tracking-widest">
                  Execution Output
                </span>
              </div>
              {(pageState === "success" || pageState === "error") && (
                <button
                  onClick={handleReset}
                  className="flex items-center gap-1.5 text-xs font-mono text-slate-600
                    hover:text-slate-400 transition-colors duration-150"
                >
                  <RefreshCw className="w-3 h-3" />
                  New deployment
                </button>
              )}
            </div>

            {pageState === "loading" && <AiProcessingLoader />}
            {pageState === "success" && result && (
                <ResultPanel 
                result={result} 
                onExecute={handleExecutePlan}
                isExecuting={isExecuting}
                executeMessage={executeMessage}
                />
            )}
            {pageState === "error" && <ErrorPanel message={errorMessage} onRetry={handleReset} />}
          </div>
        )}

        <div className="flex items-center justify-between pt-2 border-t border-slate-800/60">
          <p className="text-xs font-mono text-slate-700">
            mindops-cloud · orchestrator v1.0
          </p>
          <p className="text-xs font-mono text-slate-700">
            powered by Gemini AI
          </p>
        </div>
      </div>
    </div>
  );
}