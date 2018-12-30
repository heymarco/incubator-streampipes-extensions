/*
 * Copyright 2018 FZI Forschungszentrum Informatik
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.streampipes.processors.imageprocessing.jvm.processor.imageenrichment;

import org.streampipes.model.graph.DataProcessorInvocation;
import org.streampipes.wrapper.routing.SpOutputCollector;
import org.streampipes.wrapper.standalone.engine.StandaloneEventProcessorEngine;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.List;

public class ImageEnricher extends StandaloneEventProcessorEngine<ImageEnrichmentParameters> {

    private ImageEnrichmentParameters params;

    public ImageEnricher(ImageEnrichmentParameters params) {
        super(params);
    }

    @Override
    public void onInvocation(ImageEnrichmentParameters params, DataProcessorInvocation graph) {
        this.params = params;
    }

    @Override
    public void onEvent(Map<String, Object> in, String s, SpOutputCollector out) {

        List<Map<String, Object>> allBoxes = (List<Map<String, Object>>) in.get(params.getBoxArray());

        Optional<BufferedImage> imageOpt = getImage(in.get(params.getImageProperty()));

        if (imageOpt.isPresent()) {
            BufferedImage image = imageOpt.get();

            for (Map<String, Object> box : allBoxes) {
//
                BoxCoordinates boxCoordinates = getBoxCoordinates(image, box);

                Graphics2D graph = image.createGraphics();
                int alpha = 180;
                Color color = new Color(133, 148, 229, alpha);
                graph.setColor(color);
                graph.fill(new Rectangle(boxCoordinates.getX(), boxCoordinates.getY(), boxCoordinates.getWidth(),
                        boxCoordinates.getHeight()));
                graph.dispose();

            }

            // TODO howto change final image
            Optional<byte[]> finalImage = makeImage(image);

            if (finalImage.isPresent()) {
                Map<String, Object> outMap = new HashMap<>();
                outMap.put("image", Base64.getEncoder().encodeToString(finalImage.get()));
                out.onEvent(outMap);
            }
        }

    }

    private BoxCoordinates getBoxCoordinates(BufferedImage image, Map<String, Object> box) {
        Float x = toFloat(box.get(params.getBoxX()));
        Float y = toFloat(box.get(params.getBoxY()));
        Float width = toFloat(box.get(params.getBoxWidth()));
        Float height = toFloat(box.get(params.getBoxHeight()));

        return BoxCoordinates.make(image.getWidth(), image.getHeight(), width, height, x, y);
      }

  private Float toFloat(Object obj) {
    return Float.parseFloat(String.valueOf(obj));
  }


    private Optional<byte[]> makeImage(BufferedImage image) {

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", baos);
            baos.flush();
            byte[] finalImage = baos.toByteArray();
            baos.close();
            return Optional.of(finalImage);
        } catch (IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }

    }


    private Optional<BufferedImage> getImage(Object image) {
        String imageBase64 = String.valueOf(image);

        InputStream img = new ByteArrayInputStream(Base64.getDecoder().decode(imageBase64));
        try {
            return Optional.of(ImageIO.read(img));
        } catch (IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }
    @Override
    public void onDetach() {

    }


}