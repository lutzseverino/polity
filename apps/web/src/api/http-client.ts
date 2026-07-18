import axios, {
  AxiosHeaders,
  type AxiosHeaderValue,
  type AxiosInstance,
  type AxiosRequestConfig,
} from "axios";

type HttpClientOptions = Readonly<{
  axiosConfig?: AxiosRequestConfig;
  baseUrl?: string;
}>;

type HttpRequestConfig<RequestData = unknown> = Omit<
  AxiosRequestConfig<RequestData>,
  "baseURL" | "headers"
> &
  Readonly<{
    acceptedLanguage: string;
    headers?: AxiosHeaders | Record<string, AxiosHeaderValue>;
  }>;

export type HttpClient = Readonly<{
  request<ResponseData, RequestData = unknown>(
    config: HttpRequestConfig<RequestData>,
  ): Promise<ResponseData>;
}>;

export function createHttpClient({
  axiosConfig,
  baseUrl = "/api/v1",
}: HttpClientOptions = {}): HttpClient {
  const client: AxiosInstance = axios.create({
    ...axiosConfig,
    baseURL: baseUrl,
  });

  return {
    async request<ResponseData, RequestData = unknown>({
      acceptedLanguage,
      headers,
      ...config
    }: HttpRequestConfig<RequestData>) {
      const requestHeaders = AxiosHeaders.from(headers);
      requestHeaders.set("Accept-Language", acceptedLanguage);

      const response = await client.request<ResponseData>({
        ...config,
        headers: requestHeaders,
      });

      return response.data;
    },
  };
}

export function hasHttpResponseStatus(error: unknown, status: number) {
  return axios.isAxiosError(error) && error.response?.status === status;
}
