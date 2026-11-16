
import {
  ApplicationConfig,
  LOCALE_ID,
  provideBrowserGlobalErrorListeners
} from '@angular/core';

import { registerLocaleData } from '@angular/common';
import localeEsPe from '@angular/common/locales/es-PE';

import { provideRouter } from '@angular/router';

import {
  provideHttpClient,
  withInterceptors
} from '@angular/common/http';

import {
  ArcElement,
  BarController,
  BarElement,
  CategoryScale,
  DoughnutController,
  Legend,
  LinearScale,
  LineController,
  LineElement,
  PointElement,
  Tooltip
} from 'chart.js';

import { provideCharts } from 'ng2-charts';

import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth.interceptor';

// Registrar los datos regionales del español de Perú.
registerLocaleData(localeEsPe);

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),

    provideRouter(routes),

    provideHttpClient(
      withInterceptors([authInterceptor])
    ),

    // Registrar únicamente los elementos necesarios
    // para los gráficos de líneas, barras y dona.
    provideCharts({
      registerables: [
        ArcElement,
        BarController,
        BarElement,
        CategoryScale,
        DoughnutController,
        Legend,
        LinearScale,
        LineController,
        LineElement,
        PointElement,
        Tooltip
      ]
    }),

    {
      provide: LOCALE_ID,
      useValue: 'es-PE'
    }
  ]
};


