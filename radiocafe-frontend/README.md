# RadioCafé — frontend React

Consulta el [README principal](../README.md) para iniciar el backend, crear las cuentas demo y revisar los flujos y la arquitectura.

```powershell
npm ci
npm run dev
```

URL local: http://127.0.0.1:5173/ . El proxy `/api` requiere Spring Boot en `127.0.0.1:8080`.

`npm run build` compila para producción y `npm run lint` revisa el código. `npm run preview` permite revisar la compilación con el mismo proxy local.

La identidad viene de `/auth/me`; los datos se guardan mediante la API, no en arreglos de demostración ni en `localStorage`. El lector de QR se carga bajo demanda y requiere autorización del navegador para la cámara. Existe entrada manual de código.
