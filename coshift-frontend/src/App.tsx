import { BrowserRouter, Routes, Route } from "react-router-dom";
import MainLayout from "./layouts/MainLayout";
import HomePage from "./pages/Home/HomePage";
import ActusPage from "./pages/Actus/ActusPage";
import ArticlePage from "./pages/Actus/ArticlePage";
import LoginPage from './pages/Login/LoginPage';
import RegisterPage from "./pages/Register/RegisterPage";
import DashboardPage from "./pages/Dashboard/DashboardPage";
import VerificationPage from "./pages/Verification/VerificationPage";
import SearchTripsPage from "./pages/Trips/SearchTripsPage";
import CreateTripPage from "./pages/Trips/CreateTripPage";
import TripDetailPage from "./pages/Trips/TripDetailPage";
import MyBookingsPage from "./pages/Bookings/MyBookingsPage";
import StyleguidePage from "./pages/Styleguide/StyleguidePage";

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
          {/* Doit rester APRES la route fixe "actus". */}
          <Route path="actus/:id" element={<ArticlePage />} />
          <Route path="a-propos" element={<AboutPage />} />
          <Route path="blog" element={<BlogPage />} />
          <Route path="login" element={<LoginPage />} />
          <Route path="register" element={<RegisterPage />} />
          <Route path="verify-email" element={<VerificationPage />} />
          <Route path="dashboard" element={<DashboardPage />} />
          <Route path="trips/search" element={<SearchTripsPage />} />
          <Route path="trips/create" element={<CreateTripPage />} />
          {/* Doit rester APRÈS les routes fixes, sinon "search" et "create"
              seraient interprétés comme des identifiants de trajet. */}
          <Route path="trips/:uuid" element={<TripDetailPage />} />
          <Route path="bookings" element={<MyBookingsPage />} />
          {/* Planche des composants : reference visuelle et support du rapport. */}
          <Route path="styleguide" element={<StyleguidePage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}

export default App;
