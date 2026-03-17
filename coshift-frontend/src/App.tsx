import { BrowserRouter, Routes, Route } from "react-router-dom";
import MainLayout from "./layouts/MainLayout";
import HomePage from "./pages/Home/HomePage";
import ActusPage from "./pages/Actus/ActusPage";
import LoginPage from './pages/Login/LoginPage';
import RegisterPage from "./pages/Register/RegisterPage";

// Pages fictives temporaires pour éviter les erreurs
const EntreprisesPage = () => (
  <div style={{ padding: "5rem", textAlign: "center" }}>
    <h1>Espace Entreprises</h1>
  </div>
);
const AboutPage = () => (
  <div style={{ padding: "5rem", textAlign: "center" }}>
    <h1>À propos de nous</h1>
  </div>
);
const BlogPage = () => (
  <div style={{ padding: "5rem", textAlign: "center" }}>
    <h1>Le Blog Interne</h1>
  </div>
);

function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Le Layout englobe toutes ces routes */}
        <Route path="/" element={<MainLayout />}>
          <Route index element={<HomePage />} />
          <Route path="entreprises" element={<EntreprisesPage />} />
          <Route path="actus" element={<ActusPage />} />
          <Route path="a-propos" element={<AboutPage />} />
          <Route path="blog" element={<BlogPage />} />
          <Route path="login" element={<LoginPage />} />
          <Route path="register" element={<RegisterPage />} />
          {/* <Route path="dashboard" element={<DashboardPage />} /> */}
        </Route>
      </Routes>
    </BrowserRouter>
  );
}

export default App;
