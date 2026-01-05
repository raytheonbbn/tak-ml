/* Copyright 2025 RTX BBN Technologies */
import { Route, Routes } from 'react-router'
import './App.css'
import Error404 from './pages/Error404'
import Home from './pages/Home'
import ModelDetails from './pages/ModelDetails'

function App() {

  return (
    <>
      <Routes>
          <Route index element={<Home />} />
          <Route path="details/:modelId" element={<ModelDetails />} />
          <Route path="*" element={<Error404 />} />
      </Routes>
    </>
  )
}

export default App
