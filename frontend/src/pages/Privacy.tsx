import { useTranslation } from "../i18n/LanguageContext";
import { imprintUrl } from "../util/imprintUrl";
import "../components/Legal.css";

export default function Privacy() {
  const { t } = useTranslation();

  return (
    <article className="main-flex-container legal-page">
      <h1>{t.privacy.title}</h1>
      <p className="legal-updated">{t.privacy.updated}</p>
      <h2>{t.privacy.controllerTitle}</h2>
      <p>{t.privacy.controller} <a href={imprintUrl}>{t.legal.imprint}</a></p>
      {t.privacy.sections.map(section => (
        <section key={section.title}>
          <h2>{section.title}</h2>
          {section.paragraphs.map(paragraph => <p key={paragraph}>{paragraph}</p>)}
          {section.link && <p><a href={section.link} target="_blank" rel="noreferrer">{section.link}</a></p>}
        </section>
      ))}
    </article>
  );
}
