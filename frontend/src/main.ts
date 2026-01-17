import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { App } from './app/app';
import { isDevMode } from '@angular/core';

if (!isDevMode()) {
  // Odstraněno logování v produkčním režimu
  window.console.log = () => { };
  window.console.debug = () => { };
  window.console.trace = () => { };
}

bootstrapApplication(App, appConfig)
  .catch((err) => console.error(err));
