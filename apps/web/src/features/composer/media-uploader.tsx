import { useState, useRef } from "react";
import { cn } from "@nexora/ui";

interface MediaUploaderProps {
  onUploadSuccess: (data: {
    bucketName: string;
    objectKey: string;
    mimeType: string;
    mediaKind: string;
    sizeBytes: number;
    sha256Checksum: string;
    sourceUrl: string;
  }) => void;
  onUploadError: (error: string) => void;
  uploadMediaCall: (file: File) => Promise<any>;
}

export function MediaUploader({ onUploadSuccess, onUploadError, uploadMediaCall }: MediaUploaderProps) {
  const [isHovering, setIsHovering] = useState(false);
  const [isUploading, setIsUploading] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  async function handleFile(file: File | undefined) {
    if (!file) return;
    setIsUploading(true);
    try {
      const response = await uploadMediaCall(file);
      onUploadSuccess(response);
    } catch (error: any) {
      onUploadError(error?.response?.data?.message ?? error.message ?? "Failed to upload file");
    } finally {
      setIsUploading(false);
      if (fileInputRef.current) {
        fileInputRef.current.value = "";
      }
    }
  }

  function handleDrop(e: React.DragEvent<HTMLDivElement>) {
    e.preventDefault();
    setIsHovering(false);
    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      handleFile(e.dataTransfer.files[0]);
    }
  }

  return (
    <div
      className={cn(
        "relative flex cursor-pointer flex-col items-center justify-center rounded-3xl border-2 border-dashed p-10 text-center transition-all",
        isHovering ? "border-sky-500 bg-sky-50" : "border-slate-200 bg-slate-50 hover:bg-slate-100",
        isUploading && "pointer-events-none opacity-50"
      )}
      onClick={() => fileInputRef.current?.click()}
      onDragOver={(e) => {
        e.preventDefault();
        setIsHovering(true);
      }}
      onDragLeave={() => setIsHovering(false)}
      onDrop={handleDrop}
    >
      <input
        type="file"
        className="hidden"
        ref={fileInputRef}
        onChange={(e) => handleFile(e.target.files?.[0])}
        accept="image/*,video/*"
      />
      <div className="rounded-full bg-white p-4 shadow-sm">
        <svg
          xmlns="http://www.w3.org/2000/svg"
          fill="none"
          viewBox="0 0 24 24"
          strokeWidth={1.5}
          stroke="currentColor"
          className="h-6 w-6 text-sky-600"
        >
          <path strokeLinecap="round" strokeLinejoin="round" d="M12 16.5V9.75m0 0l3 3m-3-3l-3 3M6.75 19.5a4.5 4.5 0 01-1.41-8.775 5.25 5.25 0 0110.233-2.33 3 3 0 013.758 3.848A3.752 3.752 0 0118 19.5H6.75z" />
        </svg>
      </div>
      <p className="mt-4 text-sm font-semibold text-slate-800">
        {isUploading ? "Uploading..." : "Click to upload or drag & drop"}
      </p>
      <p className="mt-1 text-xs text-slate-500">SVG, PNG, JPG, or MP4 (max. 50MB)</p>
    </div>
  );
}
