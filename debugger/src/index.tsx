import React from 'react';
import ReactDOM from 'react-dom';
import './index.css';
import '@blueprintjs/core/lib/css/blueprint.css';
import App from './App';

ReactDOM.render(<App debugData={(window as any).palantirJavaFormat}/>, document.getElementById('root'));
