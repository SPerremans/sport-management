/* The controller exposes status only to the administrator-owned viewing session. */
(() => {
  "use strict";
  const scriptUrl = document.currentScript.src;
  const endpoint = new URL("admin-user-view/status", scriptUrl);
  const loginUrl = new URL("login.html", scriptUrl);
  async function showUserView() {
    try {
      const response = await fetch(endpoint, { credentials: "same-origin", cache: "no-store", redirect: "error" });
      if (response.status !== 200) return;
      const view = await response.json();
      if (!view.userName || !view.csrf || !view.returnPath || !Number.isFinite(view.expiresAt)) return;
      const returnUrl = new URL(view.returnPath, scriptUrl);
      if (returnUrl.origin !== location.origin) return;
      const banner = document.createElement("header");
      banner.className = "nexio-user-view-bar";
      banner.setAttribute("aria-label", "Administrator user view");
      const details = document.createElement("div");
      const title = document.createElement("strong");
      title.textContent = "Viewing as " + view.userName;
      const note = document.createElement("p");
      note.textContent = "Actions use this user's permissions and can change data. Other app tabs in this browser use the same account. Maximum 15 minutes.";
      details.append(title, note);
      const form = document.createElement("form");
      form.method = "post";
      form.action = returnUrl.href;
      const csrf = document.createElement("input");
      csrf.type = "hidden"; csrf.name = "csrf"; csrf.value = view.csrf;
      const button = document.createElement("button");
      button.type = "submit"; button.className = "btn btn-default";
      button.textContent = "Return to administrator";
      form.append(csrf, button);
      banner.append(details, form);
      document.body.prepend(banner);
      document.body.classList.add("nexio-user-view-active");
      new ResizeObserver(() => document.documentElement.style.setProperty("--nexio-user-view-height", banner.offsetHeight + "px")).observe(banner);
      setTimeout(() => {
        note.textContent = "This user view expired. Sign in again as administrator.";
        button.disabled = true;
        const signIn = document.createElement("a");
        signIn.href = loginUrl.href;
        signIn.textContent = "Sign in as administrator";
        details.append(signIn);
      }, Math.max(0, view.expiresAt - Date.now()));
    } catch (_) {
      // An unavailable development-only controller must not interfere with normal sign-in or navigation.
    }
  }
  showUserView();
})();
