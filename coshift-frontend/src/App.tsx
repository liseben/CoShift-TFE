import { BrowserRouter, Routes, Route } from "react-router-dom";
import MainLayout from "./layouts/MainLayout";
import HomePage from "./pages/Home/HomePage";
// import LoginPage from './pages/Login/LoginPage'; // On l'importera plus tard

function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Le Layout englobe toutes ces routes */}
        <Route path="/" element={<MainLayout />}>
          <Route index element={<HomePage />} />
          {/* <Route path="login" element={<LoginPage />} /> */}
          {/* <Route path="dashboard" element={<DashboardPage />} /> */}
        </Route>
      </Routes>
    </BrowserRouter>
  );
}

export default App;
