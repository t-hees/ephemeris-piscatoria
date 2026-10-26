import { Link } from "react-router-dom";
import { useTranslation } from "../i18n/LanguageContext";
import { imprintUrl } from "../util/imprintUrl";
import "./Legal.css";

export default function Footer() {
  const { t } = useTranslation();

  return (
    <footer className="legal-links">
      <Link to="/privacy">{t.legal.privacy}</Link>
      <a href={imprintUrl}>{t.legal.imprint}</a>
    </footer>
  );
}
