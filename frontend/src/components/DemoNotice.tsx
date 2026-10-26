import { useState } from "react";
import { Link, useLocation } from "react-router-dom";
import { languages, useTranslation } from "../i18n/LanguageContext";
import { imprintUrl } from "../util/imprintUrl";
import "./Legal.css";

const STORAGE_KEY = "demoNoticeAccepted";

function storedAccepted(): boolean {
  try {
    return localStorage.getItem(STORAGE_KEY) === "true";
  } catch {
    return false;
  }
}

export default function DemoNotice() {
  const [accepted, setAccepted] = useState<boolean>(storedAccepted);
  const { pathname } = useLocation();
  const { language, setLanguage, t } = useTranslation();
  const nextLanguage = languages[(languages.indexOf(language) + 1) % languages.length];

  // The privacy notice stays readable before accepting, it is linked from the notice
  if (accepted || pathname === "/privacy") return null;

  const accept = () => {
    try {
      localStorage.setItem(STORAGE_KEY, "true");
    } catch {
      // The notice then shows up again on the next visit
    }
    setAccepted(true);
  };

  return (
    <div className="demo-notice-backdrop">
      <div className="demo-notice" role="dialog" aria-modal="true" aria-labelledby="demo-notice-title">
        <div className="demo-notice-header">
          <h2 id="demo-notice-title">{t.demoNotice.title}</h2>
          <button type="button" title={t.navbar.switchLanguage} onClick={() => setLanguage(nextLanguage)}>
            {nextLanguage.toUpperCase()}
          </button>
        </div>
        <ul>
          {t.demoNotice.points.map(point => <li key={point}>{point}</li>)}
        </ul>
        <p className="legal-links">
          <Link to="/privacy">{t.legal.privacy}</Link>
          <a href={imprintUrl}>{t.legal.imprint}</a>
        </p>
        <button type="button" autoFocus onClick={accept}>
          {t.demoNotice.accept}
        </button>
      </div>
    </div>
  );
}
