import React from 'react';
import logo from './logo.svg';
import './App.css';
import { Tooltip } from "@blueprintjs/core";

const App: React.FC = () => {
  return (
    <div className="App">
      <header className="App-header">
        <img src={logo} className="App-logo" alt="logo" />
        <p>
          <Tooltip content={"yep"}>
            <span>Edit <code>src/App.tsx</code> and save to reload!</span>
          </Tooltip>
        </p>
        <a
          className="App-link"
          href="https://reactjs.org"
          target="_blank"
          rel="noopener noreferrer"
        >
          Learn React
        </a>
      </header>
    </div>
  );
}

export default App;
