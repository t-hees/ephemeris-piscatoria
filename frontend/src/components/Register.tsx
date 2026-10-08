import { useEffect, useState, type ChangeEventHandler } from "react";
import { useTranslation } from "../i18n/LanguageContext";
import { fetchApi } from "../util/fetchApi";
import { Loading } from "./Loading";
import type { NotifiableContentContext } from "./NotifiableContainer";
import { Turnstile } from "./Turnstile";

type CaptchaConfig = {
  siteKey: string | null,
}

export const Register = ({ setNotification, setError }: NotifiableContentContext) => {
  const { t } = useTranslation();
  const [loading, setLoading] = useState<boolean>(false);
  const [username, setUsername] = useState<string>("");
  const [password, setPassword] = useState<string>("");
  const [repeatPassword, setRepeatPassword] = useState<string>("");
  // Without a site key the backend doesn't require a captcha
  const [siteKey, setSiteKey] = useState<string | null>(null);
  const [captchaToken, setCaptchaToken] = useState<string | null>(null);
  const [captchaReset, setCaptchaReset] = useState<number>(0);

  useEffect(() => {
    fetchApi("auth/captcha", "GET", async (response) => setSiteKey((await response.json() as CaptchaConfig).siteKey),
      setError, () => {});
  }, [setError]);

  const requestRegister: ChangeEventHandler<HTMLFormElement> = (e) => {
    e.preventDefault();
    if (password != repeatPassword) {
      setError(t.loginPage.passwordsDontMatch);
      return;
    }
    if (siteKey && !captchaToken) {
      setError(t.loginPage.captchaMissing);
      return;
    }
    setLoading(true);
    const relPath = "auth/register";
    const jsonBody = {
      username: username,
      password: password,
      captchaToken: captchaToken,
    };
    fetchApi(relPath, "POST", handleResponse, handleError, setLoading, jsonBody);
  }

  const handleResponse = async () => {
    setNotification(t.loginPage.registerSuccess);
  }

  // The token was used up by the failed request
  const handleError = (error: string | null) => {
    setError(error);
    setCaptchaReset(count => count + 1);
  }

  return (
    <div>
      <h2>{t.loginPage.registerTitle}</h2>
      <p>{t.loginPage.demoAccountHint}</p>
      <form onSubmit={requestRegister}>
        <div>
          <label className="form-label">{t.common.username}</label>
          <input
            type="text"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            // Same rule as RegisterDto in the backend
            pattern="[A-Za-z0-9_.\-]{3,30}"
            title={t.loginPage.usernameRules}
            required
          />
        </div>
        <div>
          <label className="form-label">{t.common.password}</label>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            minLength={8}
            maxLength={72}
            required
          />
        </div>
        <div>
          <label className="form-label">{t.loginPage.repeatPassword}</label>
          <input
            type="password"
            value={repeatPassword}
            onChange={(e) => setRepeatPassword(e.target.value)}
            required
          />
        </div>
        {siteKey &&
          <Turnstile siteKey={siteKey} onToken={setCaptchaToken} resetSignal={captchaReset} onError={setError} />
        }
        {loading && <Loading />}
        <button className="form-submit-button" type="submit">{t.common.send}</button>
      </form>
    </div>
  );
}
