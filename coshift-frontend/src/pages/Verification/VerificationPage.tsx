import { useState, useRef, useEffect, type FormEvent, type KeyboardEvent, type ClipboardEvent } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { FiMail } from "react-icons/fi";
import axios from "axios";
import { useAuth } from "../../context/AuthContext";
import { API_BASE } from "../../config/api";
import { Alert, Button } from "../../components/ui";
import Logo from "../../components/Logo/Logo";
import "../Auth/auth.css";
import "./VerificationPage.css";

const LENGTH = 6;

export default function VerificationPage() {
  const [searchParams] = useSearchParams();
  const email = searchParams.get("email") ?? "";
  const navigate = useNavigate();
  const { login } = useAuth();

  const [digits, setDigits] = useState<string[]>(Array(LENGTH).fill(""));
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [cooldown, setCooldown] = useState(0);

  const refs = useRef<(HTMLInputElement | null)[]>([]);

  useEffect(() => {
    refs.current[0]?.focus();
  }, []);

  useEffect(() => {
    if (cooldown <= 0) return;
    const timer = setTimeout(() => setCooldown((c) => c - 1), 1000);
    return () => clearTimeout(timer);
  }, [cooldown]);

  const setDigit = (index: number, value: string) => {
    if (!/^\d?$/.test(value)) return;
    setDigits((prev) => {
      const next = [...prev];
      next[index] = value;
      return next;
    });
    if (value && index < LENGTH - 1) refs.current[index + 1]?.focus();
  };

  const onKeyDown = (index: number, e: KeyboardEvent) => {
    if (e.key === "Backspace" && !digits[index] && index > 0) {
      refs.current[index - 1]?.focus();
    }
    // Les flèches doivent parcourir le code : sans cela, seule la tabulation
    // permet de revenir corriger un chiffre.
    if (e.key === "ArrowLeft" && index > 0) refs.current[index - 1]?.focus();
    if (e.key === "ArrowRight" && index < LENGTH - 1) refs.current[index + 1]?.focus();
  };

  /* Le code arrive presque toujours par copier-coller depuis l'e-mail :
     on le répartit sur les six cases plutôt que de le refuser. */
  const onPaste = (e: ClipboardEvent) => {
    e.preventDefault();
    const pasted = e.clipboardData.getData("text").replace(/\D/g, "").slice(0, LENGTH);
    setDigits((prev) => {
      const next = [...prev];
      pasted.split("").forEach((ch, i) => { next[i] = ch; });
      return next;
    });
    refs.current[Math.min(pasted.length, LENGTH - 1)]?.focus();
  };

  const verify = async (e: FormEvent) => {
    e.preventDefault();
    const code = digits.join("");
    if (code.length < LENGTH) {
      setError(`Entrez les ${LENGTH} chiffres de votre code de vérification.`);
      return;
    }
    setLoading(true);
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
    } catch (err) {
      setError(
        (axios.isAxiosError(err) && err.response?.data?.message) ||
          "Code incorrect ou expiré. Réessayez.",
      );
    } finally {
      setLoading(false);
    }
  };

  const resend = async () => {
    if (cooldown > 0) return;
    setError(null);
    try {
      await axios.post(`${API_BASE}/api/auth/resend-verification`, { email });
      setSuccess("Un nouveau code vient d'être envoyé à votre adresse.");
      setCooldown(60);
    } catch (err) {
      setError(
        (axios.isAxiosError(err) && err.response?.data?.message) ||
          "Impossible d'envoyer le code. Réessayez.",
      );
    }
  };

  return (
    <div className="auth">
      <div className="auth__card">
        <header className="auth__head">
          <Logo size={36} />
          <span className="verify__icon" aria-hidden="true"><FiMail /></span>
          <h1 className="auth__title">Vérifiez votre e-mail</h1>
          <p className="auth__lead">
            Nous avons envoyé un code à {LENGTH} chiffres à{" "}
            <strong>{email || "votre adresse"}</strong>.
          </p>
        </header>

        {error && <Alert tone="danger" onDismiss={() => setError(null)}>{error}</Alert>}
        {success && <Alert tone="success">{success}</Alert>}

        <form onSubmit={verify} className="auth__form">
          <fieldset className="verify__code" onPaste={onPaste}>
            <legend className="sr-only">Code de vérification à {LENGTH} chiffres</legend>
            {digits.map((d, i) => (
              <input
                key={i}
                ref={(el) => { refs.current[i] = el; }}
                type="text"
                inputMode="numeric"
                autoComplete={i === 0 ? "one-time-code" : "off"}
                maxLength={1}
                value={d}
                onChange={(e) => setDigit(i, e.target.value)}
                onKeyDown={(e) => onKeyDown(i, e)}
                className={`verify__digit ${d ? "is-filled" : ""}`}
                disabled={loading}
                aria-label={`Chiffre ${i + 1} sur ${LENGTH}`}
              />
            ))}
          </fieldset>

          <Button type="submit" size="lg" block loading={loading}>
            Activer mon compte
          </Button>
        </form>

        <div className="auth__foot verify__foot">
          <p>Vous n'avez pas reçu de code ?</p>
          <button type="button" className="auth__link" onClick={resend} disabled={cooldown > 0}>
            {cooldown > 0 ? `Renvoyer dans ${cooldown} s` : "Renvoyer le code"}
          </button>
        </div>
      </div>
    </div>
  );
}
