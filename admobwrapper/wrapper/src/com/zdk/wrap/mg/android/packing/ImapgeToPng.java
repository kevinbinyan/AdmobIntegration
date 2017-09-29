package com.zdk.wrap.mg.android.packing;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import com.zdk.wrap.mg.ServerUtil;
import com.zdk.wrap.mg.ServerUtil.FileAction;

public class ImapgeToPng
{

	// 遍历所有名字以png结尾的文件，如果不是真正的png文件就转换成png
	public static void imToPng(String filePath) throws IOException
	{
		ServerUtil.forEachFile(filePath, new FileAction()
		{

			@Override
			public void action(File file) throws IOException
			{
				if (file.getName().endsWith("png") && !ifPng(file))
				{
//					System.out.println("++++++++++++++++++++" + file.getName());
					ConversionToPng(file.getPath());
				}
			}
		});
	}

	// 判断是否是png图片
	public static boolean ifPng(File imageFile) throws IOException
	{
		boolean ifPng = false;
		InputStream is = new FileInputStream(imageFile);
		byte[] bt = new byte[2];
		is.read(bt);

		// System.out.println(bytesToHexString(bt));
		switch (bytesToHexString(bt))
		{
			case "8950":
				// System.out.println(imageFile.getName()+" is PNG");
				ifPng = true;
				break;
			default:
				// System.out.println("default " + bytesToHexString(bt));
				ifPng = false;
		}
		return ifPng;
	}
	
	// 转换图片的代码
	public static String bytesToHexString(byte[] src)
	{
		StringBuilder stringBuilder = new StringBuilder();
		if (src == null || src.length <= 0)
		{
			return null;
		}
		for (int i = 0; i < src.length; i++)
		{
			int v = src[i] & 0xFF;
			String hv = Integer.toHexString(v);
			if (hv.length() < 2)
			{
				stringBuilder.append(0);
			}
			stringBuilder.append(hv);
		}
		return stringBuilder.toString();
	}

	// 转换图片的代码
	public static void ConversionToPng(String src)
	{
		try
		{
			File input = new File(src);
			BufferedImage bim = ImageIO.read(input);
			File output = new File(src);
			ImageIO.write(bim, "png", output);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}
