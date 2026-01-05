export const ErrorPane = ({message} : {message : string}) => {
    return (
        <div className="flex make-column flex-height-remaining width-max flex-justify-content-center flex-align-items-center error">
            {message}
        </div>
    );
}

export default ErrorPane;