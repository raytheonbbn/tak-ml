/* Copyright 2025 RTX BBN Technologies */

import { Slide, ToastContainer, toast } from "react-toastify";
import ModelsTable from "../components/ModelsTable/ModelsTable";
import TopMenu from "../components/TopMenu/TopMenu";
import { type ModelDescriptor } from "../types/Types";
import testModelDescriptors from "../data/TestModelDescriptors";
import { useEffect, useState } from "react";
import { ModelStatsDisplay } from "../components/ModelStatsDisplay/ModelStatsDisplay";
import { toModelDescriptor } from "../utilities/ApiUtils";

export const Home = () => {
    const [data, setData] = useState<ModelDescriptor[]>([]);

    const handleSuccess = (msg : string) => {
        toast.success(msg);
    }

    // const handleError = (msg : string) => {
    //     toast.error(msg);
    // }

    // ** Fetches the inference count from the server using the Metrics API,
    // then updates the model with the inference count, and adds it to the supplied
    // list of model descriptors.
    const getInferenceCountForModel = async (model : ModelDescriptor) => {
        const url : string = `/metrics/get_model_inference_count?modelName=${model.name}&modelVersion=${model.version}`;
        const promise : Promise<ModelDescriptor | void> = fetch(url)
            .then(response => {
                if (!response.ok) {
                    throw new Error(`Unable to fetch reference count for model ${model.name}`);
                }
                return response.text();
            })
            .then(text => {
                model.numInferences = Number(text);
                return model;
            })
            .catch(error => {
                console.log("Error getting inference count for model: ", error);
            });
        return promise;
    }

    const getModels = () => {
        if (import.meta.env.MODE === "production") {
            // Fetch models from the server.
            // const url : string = "/api/models/v2/get_models";
            const url : string = "/model_management/get_models";
            fetch(url)
                .then(response => {
                    if (!response.ok) {
                        throw new Error("Unable to fetch models");
                    }
                    return response.json();
                })
                .then(json => {
                
                    var newData : Array<ModelDescriptor> = [];
                    console.log("JSON: ", json);
                    console.log("Type of (JSON): ", typeof(json));
                    for (var i = 0; i < json.length; ++i) {
                        newData.push(toModelDescriptor(json[i]));
                    }
                    console.log("Setting data to: ", newData);
                    setData(newData);

                    // Update data with inference counts.
                    var newDataWithInferenceCounts : Array<ModelDescriptor> = [];
                    var promises : Array<Promise<ModelDescriptor|void>> = [];
                    newData.forEach((modelDescriptor) => {
                        console.log(`Getting inference count for model: name = ${modelDescriptor.name}, version = ${modelDescriptor.version}`);
                        const promise = getInferenceCountForModel(modelDescriptor);
                        promises.push(promise);
                    });
                    Promise.all(promises)
                        .then((values) => {
                            console.log(`${values.length} inference count calls have completed`);
                            values.forEach((value) => {
                                if (value !== null && value !== undefined) {
                                    newDataWithInferenceCounts.push(value);
                                }
                            });
                            if (newDataWithInferenceCounts.length === newData.length) {
                                console.log("Replacing old data now that inference counts have been loaded");
                                setData(newDataWithInferenceCounts);
                            } else {
                                console.warn(`New models list with inference counts (size = ${newDataWithInferenceCounts.length}) does not match original list size (size = ${newData.length})`);
                            }
                        });
                })
                .catch(error => console.error("Unable to fetch model data", error));
        } else {
            console.log("(development mode) Not fetching data from server");
            setData(() => testModelDescriptors);
        }
    }

    const handleAddModel = (model : ModelDescriptor, selectedFile : File) => {
        if (import.meta.env.MODE === "production") {
            // if (selectedFile === null) {
            //     console.error("Selected model file was null - cannot upload");
            //     toast.error("Cannot upload model - see logs");
            //     return;
            // }
            // Use multi-part form data.
            const formData : FormData = new FormData();

            // Add metadata.
            formData.append("requesterCallsign", model.createdBy);

            const runsOnServer : string = (model.runOnServer !== undefined) ? model.runOnServer.toString() : "";
            formData.append("runOnServer", runsOnServer);

            var supportedDevices : string = "";
            if (model.supportedDevices !== null && model.supportedDevices !== undefined) {
                supportedDevices = model.supportedDevices.join(",");
            }
            formData.append("supportedDevices", supportedDevices);

            // Add ML model file.
            formData.append("takmlModelWrapper", selectedFile);

            // Submit form.
            const url  : string = "/model_management/add_model_wrapper";
            // const url : string = "/model_management/ui/add_model";
            fetch(url, 
                {
                    method: 'POST',
                    body: formData
                }
            )
            .then(response => {
                if (!response.ok) {
                    throw new Error("Request failed");
                }
                return response.text();
            })
            .then(textResponse => {
                // Set the hash.
                model.hash = textResponse

                // Set the file size.
                // const sizeInMegabytes : number = selectedFile.size / BYTES_IN_MB;
                // console.log("Size in MB: ", sizeInMegabytes);
                // model.sizeMegabytes = Math.floor(sizeInMegabytes);
                
                // Update the list
                getModels();
                // setData(prev => [...prev, model]);

                toast.success("Uploaded model");
            })
            .catch(error => {
                console.log("Error uploading model: ", error);
                toast.error("Unable to upload model");
            })
        } else {
            toast.success("Uploaded model");
            setData(prev => [...prev, model]);
        }
    }

    const handleUpdateModel = (model : ModelDescriptor, selectedFile : File|null) => {
        var _data = Array.from(data);
        if (model.hash === null || model.hash === undefined) {
            console.error("Cannot update model - hash is undefined");
            toast.error("Cannot update model - hash is undefined");
            return;
        }
        if (import.meta.env.MODE === "production") {
            // Use multi-part form data.
            const formData : FormData = new FormData();

            // Add metadata.
            formData.append("requesterCallsign", model.createdBy);

            const runsOnServer : string = (model.runOnServer !== undefined) ? model.runOnServer.toString() : "";
            formData.append("runOnServer", runsOnServer);

            var supportedDevices : string = "";
            if (model.supportedDevices !== null && model.supportedDevices !== undefined) {
                supportedDevices = model.supportedDevices.join(",");
            }
            formData.append("supportedDevices", supportedDevices);

            // Add ML model file.
            if (selectedFile !== null) {
                formData.append("takmlModelWrapper", selectedFile);
            } else {
                console.log("User didn't specify a models file, so not including it in update request");
            }

            const url : string = `/model_management/edit_model_wrapper/${model.hash}`;
            fetch(url,
                {
                    method: 'POST',
                    body: formData
                }
            )
            .then(response => {
                if (!response.ok) {
                    throw new Error("Request failed");
                }
                return response.text();
            })
            .then(textResponse => {
                // Set the hash.
                model.hash = textResponse;
                
                // Update the list
                getModels();

                toast.success("Uploaded model");
            })
            .catch(error => {
                console.log("Error updating model: ", error);
                toast.error("Unable to upload model");
            })
        } else {
            const index = data.findIndex((m : ModelDescriptor) => m.hash === model.hash);
            console.log("Found match at index: ", index);
            if (index === -1) {
                console.error("Could not find matching model");
                toast.error("Cannot update model");
                return;
            }
            _data.splice(index, 1, model);
            setData(_data);
            toast.success("Updated model \"" + model.name + "\"");
        }
    }

    const handleDeleteModel = (descriptor : ModelDescriptor) => {
        if (import.meta.env.MODE === "production") {
            const url : string = `/model_management/remove_model/${descriptor.hash}`;
            fetch(url, 
                {
                    method: 'DELETE'
                }
            )
            .then(response => {
                if (!response.ok) {
                    throw new Error("Observation not deleted");
                }
                return response.text();
            })
            .then(textResponse => {
                console.log("response is ", textResponse);
                setData(prev => prev.filter(row => row.hash !== descriptor.hash));
                toast.success("Deleted model \"" + descriptor.name + "\"");
            })
            .catch(error => {
                console.log(error);
                toast.error("Unable to delete model");
            })
        } else {
            setData(prev => prev.filter(row => row.hash !== descriptor.hash));
            toast.success("Deleted model \"" + descriptor.name + "\"");
        }
    }

    // const countTotalActive = () => {
    //     return data.filter(i => i.status === ModelStatus.ACTIVE).length;
    // }

    const sumTotalStorage = () => {
        if (data.length === 0) {
            return 0;
        }
        return data.map(m => m.sizeMegabytes).reduce((accumulator, currentValue) => accumulator + currentValue);
    }

    useEffect(() => {
        getModels();
    }, []);

    return (
        <>
            <div className="flex make-column width-max">
                <TopMenu 
                    onUpload={handleAddModel}
                />
                <ModelStatsDisplay 
                    total={data.length} 
                    totalStorage={sumTotalStorage()}
                />
                <div className="flex flex-center margin-left margin-right">
                    <ModelsTable 
                        data={data}
                        /*setData={setData}*/
                        onUpdate={(modelDescriptor : ModelDescriptor, selectedFile : File|null) => handleUpdateModel(modelDescriptor, selectedFile)}
                        onDelete={(modelDescriptor : ModelDescriptor) => handleDeleteModel(modelDescriptor)}
                        onSuccess={(msg : string) => handleSuccess(msg)}
                    />
                </div>
                {/* <div className="flex make-row flex-align-items-start margin-left">
                    <UploadFileInput
                        header="Upload Model"
                        fileTypes=".pt,.tflite,.onnx,.zip"
                        onUpload={handleAddModel}
                    />
                </div> */}
                <ToastContainer 
                    position="bottom-right"
                    autoClose={2000}
                    hideProgressBar
                    newestOnTop={false}
                    closeOnClick
                    draggable={false}
                    theme="dark"
                    transition={Slide}
                />
            </div>
        </>
    )
};

export default Home;