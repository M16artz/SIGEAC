/**
 * Logging interface for the model generation library
 */
export interface ModelLoggingInterface {
    debug(message?: any, ...optionalParams: any[]): void;
    info(message?: any, ...optionalParams: any[]): void;
    warn(message?: any, ...optionalParams: any[]): void;
    error(message?: any, ...optionalParams: any[]): void;
}
/**
 * Logger class for the model generation library
 *
 * This class acts as a forefront for any external loggers which is why it also implements the interface itself.
 */
export declare class LoggerClass implements ModelLoggingInterface {
    private logger?;
    debug(message?: any, ...optionalParams: any[]): void;
    info(message?: any, ...optionalParams: any[]): void;
    warn(message?: any, ...optionalParams: any[]): void;
    error(message?: any, ...optionalParams: any[]): void;
    /**
     * Sets the logger to use for the model generation library
     *
     * @param logger to add
     */
    setLogger(logger?: ModelLoggingInterface): void;
}
export declare const Logger: LoggerClass;
//# sourceMappingURL=LoggingInterface.d.ts.map