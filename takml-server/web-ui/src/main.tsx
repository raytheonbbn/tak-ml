import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router';
import './index.css';
import './css/main.css';
import App from './App.tsx';
import { StrictMode } from 'react';

if (import.meta.env.MODE === "development") {
  console.log("Running in development mode...");
} else if (import.meta.env.MODE === "production") {
  console.log("Running in production mode...");
}

const root = document.getElementById("root");

createRoot(root!).render(
  <StrictMode>
    <BrowserRouter>
      <App />
    </BrowserRouter>
  </StrictMode>
)
