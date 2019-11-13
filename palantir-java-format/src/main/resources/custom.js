'use strict';

const e = React.createElement;

// Find all DOM containers, and render tooltips into them.
document.querySelectorAll('.tooltip')
    .forEach(domContainer => {
        const content = domContainer.title;
        ReactDOM.render(
            e(Blueprint.Core.Tooltip, { content: content }),
            domContainer
        );
    });