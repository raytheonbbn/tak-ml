/* Copyright 2025 RTX BBN Technologies */
import { type ChangeEvent, useEffect, useRef } from 'react';
import "./UploadModelPopupFileInput.css";

export const UploadModelPopupFileInput = ({header, fileTypes, setSelectedFile, setFileUploadDisabled} : 
    {header : string, fileTypes : any, setSelectedFile : (f : File|null) => void, setFileUploadDisabled : (x : boolean) => void})=> {
    const fileInputRef = useRef<HTMLInputElement>(null);

    const createFileInputId = (name : string) => {
        return name.toLowerCase().replace(" ", "-");
    }

    const onFileChange = (event : ChangeEvent<HTMLInputElement>) => {
        if (event.target.files !== undefined && event.target.files !== null && event.target.files.length > 0) {
            console.log("Setting image file to: " + event.target.files[0].name + ", (MIME type = " + event.target.files[0].type + ")");
            setSelectedFile(event.target.files[0]);
            setFileUploadDisabled(false);
        } else {
            console.log("No files are selected");
            setFileUploadDisabled(true);
        }
    }

    useEffect(() => {
        const handleCanceled = () => {
            console.log("File input selection canceled");
            setFileUploadDisabled(true);
        }
        const fileInput = fileInputRef.current;
        if (fileInput) {
            fileInput.addEventListener('cancel', handleCanceled);
            return () => {
                fileInput.removeEventListener('cancel', handleCanceled);
            }
        }
    }, [])

    return (
        // <div className="flex make-column flex-center margin">
            <div className="flex flex-align-items-start flex-justify-content-start make-column margin"> 
                <div className="flex make-row">
                    <input 
                        id={createFileInputId(header)}
                        ref={fileInputRef}
                        name="ml-file"
                        className="upload-file-input"
                        type="file" 
                        accept={fileTypes} 
                        onChange={(e) => {
                            if (e !== null && e !== undefined && e.target !== null && e.target !== undefined) {
                                onFileChange(e);
                            } else {
                                console.log("No files are selected");
                                setFileUploadDisabled(true);
                            }
                        }}
                    />
                </div>
            </div>
        // </div>
    );
}

export default UploadModelPopupFileInput;