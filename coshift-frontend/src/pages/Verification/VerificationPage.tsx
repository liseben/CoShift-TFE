import React, { useState, useRef, useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import axios from "axios";
import "./VerificationPage.css";

import { API_BASE } from "../../config/api";

const VerificationPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const email = searchParams.get("email") ?? "";
  const navigate = useNavigate();
  const { login } = useAuth();

  const [digits, setDigits] = useState<string[]>(["", "", "", "", "", ""]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [resendCooldown, setResendCooldown] = useState(0);

  const inputRefs = useRef<(HTMLInputElement | null)[]>([]);

  useEffect(() => {
    inputRefs.current[0]?.focus();
  }, []);

  useEffect(() => {
    if (resendCooldown <= 0) return;
    const timer = setTimeout(() => setResendCooldown((c) => c - 1), 1000);
    return () => clearTimeout(timer);
  }, [resendCooldown]);

  const handleDigitChange = (index: number, value: string) => {
    if (!/^\d?$/.test(value)) return;
    const next = [...digits];
    next[index] = value;
    setDigits(next);
    if (value && index < 5) {
      inputRefs.current[index + 1]?.focus();
    }
  };

  const handleKeyDown = (index: number, e: React.KeyboardEvent) => {
    if (e.key === "Backspace" && !digits[index] && index > 0) {
      inputRefs.current[index - 1]?.focus();
    }
  };

  const handlePaste = (e: React.ClipboardEvent) => {
    e.preventDefault();
    const pasted = e.clipboardData.getData("text").replace(/\D/g, "").slice(0, 6);
    const next = [...digits];
    pasted.split("").forEach((ch, i) => { next[i] = ch; });
    setDigits(next);
    const lastFilled = Math.min(pasted.length, 5);
    inputRefs.current[lastFilled]?.focus();
  };

  const handleVerify = async (e: React.FormEvent) => {
    e.preventDefault();
    const code = digits.join("");
    if (code.length < 6) {
      setError("Entrez les 6 chiffres de votre code de vérification.");
      return;
    }
    setIsLoading(true);
    setError(null);
    setSuccess(null);
    try {
      const res = await axios.post(`${API_BASE}/api/auth/verify-email`, { email, code });
      if (res.data.token) {
        login(res.data.token);
        navigate("/dashboard", { replace: true });
      } else {
        setSuccess(res.data.message);
      }
    } catch (err: any) {
      setError(err.response?.data?.message ?? "Code incorrect ou expiré. Réessayez.");
    } finally {
      setIsLoading(false);
    }
  };

  const handleResend = async () => {
    if (resendCooldown > 0) return;
    setError(null);
    try {
      await axios.post(`${API_BASE}/api/auth/resend-verification`, { email });
      setSuccess("Un nouveau code a été envoyé à votre adresse email !");
      setResendCooldown(60);
    } catch (err: any) {
      setError(err.response?.data?.message ?? "Impossible d'envoyer le code. Réessayez.");
    }
  };

  return (
    <div className="verify-container">
      <div className="verify-card">

        <div className="verify-logo">CoShift</div>

        <div className="verify-icon-wrapper">✉️</div>

        <h1 className="verify-title">Vérifiez votre email</h1>
        <p className="verify-subtitle">
          Nous avons envoyé un code à 6 chiffres à<br />
          <strong>{email || "votre adresse email"}</strong>
        </p>

        {error && (
          <div className="verify-alert error">
            <span>⚠️</span> {error}
          </div>
        )}
        {success && (
          <div className="verify-alert success">
            <span>✓</span> {success}
          </div>
        )}

        <form onSubmit={handleVerify}>
          <div className="code-inputs" onPaste={handlePaste}>
            {digits.slice(0, 3).map((d, i) => (
              <input
                key={i}
                ref={(el) => { inputRefs.current[i] = el; }}
                type="text"
                inputMode="numeric"
                maxLength={1}
                value={d}
                onChange={(e) => handleDigitChange(i, e.target.value)}
                onKeyDown={(e) => handleKeyDown(i, e)}
                className={`digit-input ${d ? "filled" : ""}`}
                disabled={isLoading}
              />
            ))}
            <span className="digit-separator">—</span>
            {digits.slice(3).map((d, i) => (
              <input
                key={i + 3}
                ref={(el) => { inputRefs.current[i + 3] = el; }}
                type="text"
                inputMode="numeric"
                maxLength={1}
                value={d}
                onChange={(e) => handleDigitChange(i + 3, e.target.value)}
                onKeyDown={(e) => handleKeyDown(i + 3, e)}
                className={`digit-input ${d ? "filled" : ""}`}
                disabled={isLoading}
              />
            ))}
          </div>

          <button type="submit" className="verify-btn" disabled={isLoading}>
            {isLoading && <span className="spinner" />}
            {isLoading ? "Vérification en cours..." : "Activer mon compte"}
          </button>
        </form>

        <div className="verify-footer">
          <p>Vous n'avez pas reçu de code ?</p>
          <button
            onClick={handleResend}
            className="resend-btn"
            disabled={resendCooldown > 0}
          >
            {resendCooldown > 0
              ? `Renvoyer dans ${resendCooldown}s`
              : "Renvoyer le code par email"}
          </button>
        </div>

      </div>
    </div>
  );
};

export default VerificationPage;
