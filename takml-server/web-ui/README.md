# Model Management Platform Frontend

The frontend for the Model Management Platform, built using ReactJS + TypeScript + Vite.

## Features
- Models table view, with the ability to add/update/delete models.
- Metrics page with charts for model usage over a 24-hour period and average performance by device.
- Feedback page with "top reporters" pie chart and accuracy percentage.

## Usage
1. In `web-ui/`, run `./buildAndCopyReactJs.sh`. This copies the JS files into `application/src/main/resources/static`.
2. Build and start the server as before.
3. To view the web UI, visit: `localhost:8234`.
